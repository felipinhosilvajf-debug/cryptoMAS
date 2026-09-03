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

	private int _skill1 = 0;
	private int _skill2 = 0;
	private int _skill3 = 0;

	private int _currentSkill = 1;

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
		 * Não tenta outra ação enquanto o personagem
		 * estiver executando uma ação de combate.
		 *
		 * O próprio Core controla o tempo real através
		 * do Attack Speed / Casting Speed.
		 */
		if (_player.isCastingNow())
			return;

		if (_player.isAttackingNow())
			return;

		MonsterInstance target = findNearestMonster();

		if (target == null)
			return;

		if (_player.getTarget() != target)
			_player.setTarget(target);

		if (!_player.isInRange(target, 2000))
			return;

		/*
		 * Primeiro tenta uma skill.
		 *
		 * Se nenhum dos 3 slots tiver uma skill válida,
		 * simplesmente faz ataque normal.
		 */
		if (useAutoSkill(target))
			return;

		if (!target.isDead())
		{
			_player.doAttack(target);
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

			/*
			 * 0 significa "sem skill".
			 */
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
				if (_player.isAttackingNow())
					_player.abortAttack(false, false);

				_player.doCast(skill, _player, true);

				nextSkill(index);
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
			 * Interrompe o ataque físico antes de lançar
			 * a skill.
			 */
			if (_player.isAttackingNow())
				_player.abortAttack(false, false);

			/*
			 * Usa o sistema NORMAL de cast do Core.
			 *
			 * O doCast() calcula o tempo usando:
			 * - Casting Speed
			 * - tempo da própria skill
			 * - tempo mínimo configurado
			 * - reuse da skill
			 */
			_player.doCast(skill, target, true);

			nextSkill(index);
			return true;
		}

		return false;
	}

	private void nextSkill(int index)
	{
		_currentSkill = index + 2;

		if (_currentSkill > 3)
			_currentSkill = 1;
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
