package l2f.gameserver.autofarm;

import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.instances.MonsterInstance;

import java.util.concurrent.ScheduledFuture;

public class AutoFarmTask implements Runnable
{
	private final Player _player;
	private ScheduledFuture<?> _task;

	private int _searchRadius = 1000;

    private int _skill1 = 101; // Slot 1 - Stun Shot
    private int _skill2 = 19;  // Slot 2 - Double Shot
    private int _skill3 = 4;   // Slot 3 - Dash

	private int _currentSkill = 1;

	/*
	 * Momento em que o Auto Farm poderá executar
	 * uma nova ação.
	 */
	private long _actionEndTime = 0;

	public AutoFarmTask(Player player)
	{
		_player = player;
	}

	public void start()
	{
		if (_task != null)
			return;

		_task = ThreadPoolManager.getInstance().scheduleAtFixedRate(this, 1000, 100);
	}

	public void stop()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}

		_actionEndTime = 0;
	}

	public void setSkills(int skill1, int skill2, int skill3)
	{
		_skill1 = skill1;
		_skill2 = skill2;
		_skill3 = skill3;
	}

    public int getSkill1()
    {
        return _skill1;
    }

    public int getSkill2()
    {
        return _skill2;
    }

    public int getSkill3()
    {
        return _skill3;
    }

    public void setSearchRadius(int radius)
    {
        _searchRadius = radius;
    }

    public int getSearchRadius()
    {
        return _searchRadius;
    }

	@Override
	public void run()
	{
		if (_player == null || !_player.isOnline())
		{
			stop();
			return;
		}

		if (_player.isDead())
			return;

		/*
		 * O Auto Farm está esperando a ação anterior terminar.
		 */
		if (System.currentTimeMillis() < _actionEndTime)
			return;

		/*
		 * Se o servidor ainda considera que o personagem
		 * está lançando uma skill, esperamos.
		 */
		if (_player.isCastingNow())
			return;

		MonsterInstance target = findNearestMonster();

		if (target == null)
			return;

		if (_player.getTarget() != target)
			_player.setTarget(target);

		if (!_player.isInRange(target, 2000))
			return;

		/*
		 * PRIMEIRO tenta uma skill.
		 *
		 * Se uma skill for iniciada, não existe ataque
		 * físico neste ciclo.
		 */
		if (useAutoSkill(target))
			return;

		/*
		 * Só chegamos aqui quando nenhuma skill
		 * pôde ser utilizada.
		 */
		if (!target.isDead())
		{
			_player.doAttack(target);

			/*
			 * O ataque físico passa a ser a ação atual.
			 * Esperamos o estado atual de ataque terminar
			 * antes de tentar outra skill.
			 */
			_actionEndTime = System.currentTimeMillis() + getAttackWaitTime();
		}
	}

	/**
	 * Tenta utilizar uma única skill por vez.
	 */
	private boolean useAutoSkill(MonsterInstance target)
	{
		int[] skills =
		{
			_skill1,
			_skill2,
			_skill3
		};

		for (int i = 0; i < skills.length; i++)
		{
			int index = (_currentSkill - 1 + i) % skills.length;
			int skillId = skills[index];

			if (skillId <= 0)
				continue;

			Skill skill = _player.getKnownSkill(skillId);

			if (skill == null)
				continue;

			if (_player.isSkillDisabled(skill))
				continue;

			/*
			 * DASH.
			 */
			if (skillId == 4)
			{
				/*
				 * Cancela o ataque físico antes da skill.
				 */
				if (_player.isAttackingNow())
					_player.abortAttack(false, false);

				_player.altUseSkill(skill, _player);

				/*
				 * Aguarda o tempo real de execução da skill.
				 */
				_actionEndTime = System.currentTimeMillis() + skill.getHitTime();

				_currentSkill = index + 2;

				if (_currentSkill > 3)
					_currentSkill = 1;

				return true;
			}

			/*
			 * Skill ofensiva.
			 */
			if (target == null || target.isDead())
				continue;

			if (!skill.isOffensive())
				continue;

			if (!_player.isInRange(target, skill.getCastRange()))
				continue;

			/*
			 * MUITO IMPORTANTE:
			 *
			 * Antes de lançar a skill, interrompemos
			 * o ataque físico automático.
			 */
			if (_player.isAttackingNow())
				_player.abortAttack(false, false);

			/*
			 * Lança a skill pelo sistema original.
			 *
			 * O próprio Mythras agenda o efeito para
			 * skill.getHitTime().
			 */
			_player.altUseSkill(skill, target);

			/*
			 * Bloqueia qualquer nova ação até o momento
			 * em que o efeito da skill deverá acontecer.
			 */
			_actionEndTime = System.currentTimeMillis() + skill.getHitTime();

			_currentSkill = index + 2;

			if (_currentSkill > 3)
				_currentSkill = 1;

			return true;
		}

		return false;
	}

	/**
	 * Pequena janela baseada no ataque normal.
	 *
	 * O objetivo aqui é impedir que o Auto Farm
	 * imediatamente tente uma skill enquanto o
	 * ataque físico ainda está sendo processado.
	 */
	private long getAttackWaitTime()
	{
		if (_player.isAttackingNow())
			return 1000;

		return 500;
	}

	private MonsterInstance findNearestMonster()
	{
		MonsterInstance nearest = null;
		double nearestDistance = _searchRadius;

		for (l2f.gameserver.model.instances.NpcInstance npc : GameObjectsStorage.getAllNpcs())
		{
			if (!(npc instanceof MonsterInstance))
				continue;

			MonsterInstance monster = (MonsterInstance) npc;

			if (monster.isDead())
				continue;

			if (!monster.isVisible())
				continue;

			double distance = _player.getDistance(monster);

			if (distance <= nearestDistance)
			{
				nearestDistance = distance;
				nearest = monster;
			}
		}

		return nearest;
	}
}
