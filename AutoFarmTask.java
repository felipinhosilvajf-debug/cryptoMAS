package l2f.gameserver.autofarm;

import java.util.concurrent.ScheduledFuture;

import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.GameObject;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.instances.MonsterInstance;
import l2f.gameserver.model.items.ItemInstance;
import l2f.gameserver.templates.item.WeaponTemplate;
import l2f.gameserver.templates.item.WeaponTemplate.WeaponType;

public class AutoFarmTask implements Runnable
{
	private final Player _player;
	private ScheduledFuture<?> _task;

	/*
	 * Raio máximo para procurar monstros.
	 */
	private int _searchRadius = 1000;

	/*
	 * Skills configuradas.
	 *
	 * 0 = Sem Skill.
	 */
	private int _skill1 = 0;
	private int _skill2 = 0;
	private int _skill3 = 0;

	private int _currentSkill = 1;

	/*
	 * Item que o Auto Farm está tentando coletar.
	 */
	private ItemInstance _lootTarget;

	/*
	 * Evita ficar tentando pegar o mesmo item
	 * infinitamente caso ele não possa ser coletado.
	 */
	private int _lastLootObjectId = 0;

	private long _lastLootAttempt = 0;

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

		_lootTarget = null;
		_lastLootObjectId = 0;
		_lastLootAttempt = 0;
	}

	public void setSkills(int skill1, int skill2, int skill3)
	{
		_skill1 = skill1;
		_skill2 = skill2;
		_skill3 = skill3;

		/*
		 * Se todos os slots forem alterados,
		 * começamos novamente pela Skill 1.
		 */
		_currentSkill = 1;
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
		 * Nunca tenta fazer outra ação enquanto estiver
		 * executando uma skill.
		 */
		if (_player.isCastingNow())
			return;

		/*
		 * =========================================================
		 * 1. LOOT
		 * =========================================================
		 *
		 * Primeiro tentamos pegar itens próximos.
		 *
		 * Isso faz o personagem:
		 *
		 * MATOU -> PROCURA DROP -> VAI ATÉ O DROP -> PEGA
		 *       -> VOLTA A PROCURAR MONSTRO
		 */
		ItemInstance loot = findNearestLoot();

		if (loot != null)
		{
			if (handleLoot(loot))
				return;
		}

		/*
		 * =========================================================
		 * 2. MONSTRO
		 * =========================================================
		 */
		MonsterInstance target = findNearestMonster();

		if (target == null)
			return;

		if (target.isDead())
			return;

		if (_player.getTarget() != target)
			_player.setTarget(target);

		/*
		 * =========================================================
		 * 3. SKILLS
		 * =========================================================
		 *
		 * Se existir uma skill configurada, tenta utilizá-la.
		 *
		 * Se o personagem estiver longe:
		 * - skill de longo alcance -> aproxima até o alcance;
		 * - skill melee -> aproxima até o alcance físico.
		 */
		if (useAutoSkill(target))
			return;

		/*
		 * =========================================================
		 * 4. ATAQUE NORMAL
		 * =========================================================
		 */
		if (!target.isDead())
			handleNormalAttack(target);
	}

	/**
	 * Processa o ataque normal.
	 */
	private void handleNormalAttack(MonsterInstance target)
	{
		if (target == null || target.isDead())
			return;

		/*
		 * Não tenta iniciar outro ataque enquanto o
		 * intervalo real da arma ainda estiver ativo.
		 */
		if (_player.isAttackingNow())
			return;

		if (_player.isAttackingDisabled())
			return;

		/*
		 * Obtém o alcance físico REAL do personagem.
		 *
		 * Não usamos mais 2000.
		 */
		int attackRange = getPhysicalAttackRange();

		double distance = _player.getDistance(target);

		/*
		 * Fora do alcance:
		 * movimenta até o monstro.
		 */
		if (distance > attackRange)
		{
			moveToAttackRange(target, attackRange);
			return;
		}

		/*
		 * Já chegou ao alcance.
		 */
		_player.doAttack(target);
	}

	/**
	 * Obtém o alcance correto do ataque físico.
	 *
	 * Arco/Crossbow:
	 * utiliza o alcance da arma.
	 *
	 * Melee:
	 * utiliza o POWER_ATTACK_RANGE calculado pelo Core.
	 */
	private int getPhysicalAttackRange()
	{
		WeaponTemplate weapon = _player.getActiveWeaponItem();

		if (weapon != null)
		{
			WeaponType type = weapon.getItemType();

			if (type == WeaponType.BOW ||
				type == WeaponType.CROSSBOW)
			{
				return Math.max(10, weapon.getAttackRange());
			}
		}

		return Math.max(10, _player.getPhysicalAttackRange());
	}

	/**
	 * Faz o personagem caminhar até o alcance de ataque.
	 */
	private void moveToAttackRange(MonsterInstance target, int range)
	{
		if (target == null || target.isDead())
			return;

		/*
		 * Não tenta movimentar enquanto está atacando.
		 */
		if (_player.isAttackingNow())
			return;

		if (_player.isCastingNow())
			return;

		/*
		 * Pequena margem para evitar ficar oscilando
		 * exatamente no limite do alcance.
		 */
		int offset = Math.max(10, range - 5);

		_player.followToCharacter(target, offset, false);
	}

	/**
	 * Tenta executar uma das três skills configuradas.
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
			 * 0 = Sem Skill.
			 */
			if (skillId <= 0)
				continue;

			Skill skill = _player.getKnownSkill(skillId);

			if (skill == null)
				continue;

			if (_player.isSkillDisabled(skill))
				continue;

			/*
			 * =====================================================
			 * DASH
			 * =====================================================
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
			 * Só usamos skills ofensivas no Auto Farm.
			 */
			if (target == null || target.isDead())
				continue;

			if (!skill.isOffensive())
				continue;

			/*
			 * =====================================================
			 * ALCANCE DA SKILL
			 * =====================================================
			 */
			int castRange = Math.max(10, skill.getCastRange());

			double distance = _player.getDistance(target);

			/*
			 * Está longe demais para a skill.
			 *
			 * Vamos caminhar até o alcance da própria skill.
			 */
			if (distance > castRange)
			{
				if (!_player.isAttackingNow())
				{
					int offset = Math.max(10, castRange - 5);

					_player.followToCharacter(
						target,
						offset,
						false
					);
				}

				return true;
			}

			/*
			 * Interrompe ataque normal antes de lançar a skill.
			 */
			if (_player.isAttackingNow())
				_player.abortAttack(false, false);

			/*
			 * Usa o sistema normal de CAST do Core.
			 *
			 * Isso respeita Cast Speed e os tempos
			 * calculados pelo próprio servidor.
			 */
			_player.doCast(skill, target, true);

			nextSkill(index);

			return true;
		}

		return false;
	}

	/**
	 * Passa para a próxima skill.
	 */
	private void nextSkill(int index)
	{
		_currentSkill = index + 2;

		if (_currentSkill > 3)
			_currentSkill = 1;
	}

	/**
	 * Procura o item mais próximo do jogador.
	 *
	 * Utilizamos GameObjectsStorage.getAllObjects(),
	 * pois ItemInstance fica armazenado no STORAGE_OTHER.
	 */
	private ItemInstance findNearestLoot()
	{
		ItemInstance nearest = null;
		double nearestDistance = _searchRadius;

		for (GameObject object : GameObjectsStorage.getAllObjects())
		{
			if (!(object instanceof ItemInstance))
				continue;

			ItemInstance item = (ItemInstance) object;

			if (!item.isVisible())
				continue;

			/*
			 * Não pega item muito distante.
			 */
			double distance = _player.getDistance(item);

			if (distance > nearestDistance)
				continue;

			/*
			 * Não seleciona novamente imediatamente
			 * um item que acabou de falhar.
			 */
			if (item.getObjectId() == _lastLootObjectId)
			{
				if (System.currentTimeMillis() - _lastLootAttempt < 2000)
					continue;
			}

			nearestDistance = distance;
			nearest = item;
		}

		return nearest;
	}

	/**
	 * Processa a aproximação e coleta do item.
	 */
	private boolean handleLoot(ItemInstance item)
	{
		if (item == null || !item.isVisible())
		{
			_lootTarget = null;
			return false;
		}

		_lootTarget = item;

		double distance = _player.getDistance(item);

		/*
		 * Distância de interação/pickup.
		 */
		final int PICKUP_RANGE = 200;

		/*
		 * Ainda está longe.
		 *
		 * Move até o local do drop.
		 */
		if (distance > PICKUP_RANGE)
		{
			_player.moveToLocation(
				item.getLoc(),
				100,
				true
			);

			return true;
		}

		/*
		 * Chegou no drop.
		 */
		_player.setTarget(item);

		_lastLootObjectId = item.getObjectId();
		_lastLootAttempt = System.currentTimeMillis();

		/*
		 * Usa o método original do Player.
		 *
		 * Ele já verifica:
		 * - item visível;
		 * - propriedade do drop;
		 * - party;
		 * - capacidade do inventário;
		 * - herbs;
		 * - attachments;
		 * - pickupMe().
		 */
		_player.doPickupItem(item);

		_lootTarget = null;

		return true;
	}

	/**
	 * Procura o monstro vivo mais próximo dentro do raio configurado.
	 */
	private MonsterInstance findNearestMonster()
	{
		MonsterInstance nearest = null;
		double nearestDistance = _searchRadius;

		for (l2f.gameserver.model.instances.NpcInstance npc :
			GameObjectsStorage.getAllNpcs())
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
