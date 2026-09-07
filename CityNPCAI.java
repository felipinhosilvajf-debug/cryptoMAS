package l2f.gameserver.ai;

import java.util.ArrayList;
import java.util.List;

import l2f.gameserver.model.Creature;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.utils.Location;

public class CityNpcAI extends CharacterAI
{
	private static final int TALK_TIMER = 1;
	private static final int WALK_TIMER = 2;

	/*
	 * Configurações padrão.
	 *
	 * Elas serão usadas somente se o NPC não possuir
	 * configuração própria no XML.
	 */
	private static final long DEFAULT_FIRST_TALK = 10000L;
	private static final long DEFAULT_TALK_MIN = 15000L;
	private static final long DEFAULT_TALK_MAX = 30000L;
	private static final long DEFAULT_WALK_START = 3000L;
	private static final long DEFAULT_WALK_PAUSE = 2000L;
	private static final long DEFAULT_WALK_BLOCKED = 3000L;

	private static final String[] DEFAULT_PHRASES =
	{
		"Olá, aventureiro!",
		"Que movimento hoje!",
		"Boa viagem, aventureiro!",
		"Tenha um bom dia!",
		"Está procurando alguma coisa?"
	};

	/*
	 * Rota e frases deste NPC.
	 */
	private Location[] _route;
	private String[] _phrases;

	/*
	 * Configurações individuais.
	 */
	private long _firstTalk;
	private long _talkMin;
	private long _talkMax;
	private long _walkStart;
	private long _walkPause;
	private long _walkBlocked;

	/*
	 * Ponto atual da rota.
	 */
	private int _routeIndex = 0;

	public CityNpcAI(Creature actor)
	{
		super(actor);

		loadConfiguration();
	}

	/**
	 * Carrega a configuração diretamente dos parâmetros
	 * do NPC.
	 */
	private void loadConfiguration()
	{
		Creature actor = getActor();

		if (!(actor instanceof NpcInstance))
		{
			_route = new Location[0];
			_phrases = DEFAULT_PHRASES;

			_firstTalk = DEFAULT_FIRST_TALK;
			_talkMin = DEFAULT_TALK_MIN;
			_talkMax = DEFAULT_TALK_MAX;
			_walkStart = DEFAULT_WALK_START;
			_walkPause = DEFAULT_WALK_PAUSE;
			_walkBlocked = DEFAULT_WALK_BLOCKED;

			return;
		}

		NpcInstance npc = (NpcInstance) actor;

		/*
		 * =====================================================
		 * FRASES
		 * =====================================================
		 *
		 * Exemplo:
		 *
		 * city_phrases =
		 * "Olá!;Boa viagem!;Que dia bonito!"
		 */
		String phrases = npc.getParameter("city_phrases", "");

		if (phrases != null && !phrases.trim().isEmpty())
		{
			_phrases = parsePhrases(phrases);
		}
		else
		{
			_phrases = DEFAULT_PHRASES;
		}

		/*
		 * =====================================================
		 * ROTA
		 * =====================================================
		 *
		 * Exemplo:
		 *
		 * 82808,147976,-3494;
		 * 82792,149240,-3494;
		 * 81144,149224,-3494;
		 * 81128,147832,-3494
		 */
		String route = npc.getParameter("city_route", "");

		if (route != null && !route.trim().isEmpty())
		{
			_route = parseRoute(route);
		}
		else
		{
			_route = new Location[0];
		}

		/*
		 * =====================================================
		 * TEMPOS
		 * =====================================================
		 */

		_firstTalk = getLongParameter(
			npc,
			"city_first_talk",
			DEFAULT_FIRST_TALK
		);

		_talkMin = getLongParameter(
			npc,
			"city_talk_min",
			DEFAULT_TALK_MIN
		);

		_talkMax = getLongParameter(
			npc,
			"city_talk_max",
			DEFAULT_TALK_MAX
		);

		_walkStart = getLongParameter(
			npc,
			"city_walk_start",
			DEFAULT_WALK_START
		);

		_walkPause = getLongParameter(
			npc,
			"city_walk_pause",
			DEFAULT_WALK_PAUSE
		);

		_walkBlocked = getLongParameter(
			npc,
			"city_walk_blocked",
			DEFAULT_WALK_BLOCKED
		);

		/*
		 * Segurança para evitar valores inválidos.
		 */
		if (_talkMin < 1000L)
			_talkMin = 1000L;

		if (_talkMax < _talkMin)
			_talkMax = _talkMin;

		if (_firstTalk < 0L)
			_firstTalk = DEFAULT_FIRST_TALK;

		if (_walkStart < 0L)
			_walkStart = DEFAULT_WALK_START;

		if (_walkPause < 0L)
			_walkPause = DEFAULT_WALK_PAUSE;

		if (_walkBlocked < 1000L)
			_walkBlocked = DEFAULT_WALK_BLOCKED;
	}

	/**
	 * Lê um parâmetro numérico do NPC.
	 */
	private long getLongParameter(
		NpcInstance npc,
		String parameter,
		long defaultValue)
	{
		String value = npc.getParameter(parameter, "");

		if (value == null || value.trim().isEmpty())
		{
			return defaultValue;
		}

		try
		{
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	/**
	 * Converte:
	 *
	 * "Frase 1;Frase 2;Frase 3"
	 *
	 * em um array de frases.
	 */
	private String[] parsePhrases(String value)
	{
		String[] parts = value.split(";");

		List<String> phrases = new ArrayList<String>();

		for (String part : parts)
		{
			String phrase = part.trim();

			if (!phrase.isEmpty())
			{
				phrases.add(phrase);
			}
		}

		if (phrases.isEmpty())
		{
			return DEFAULT_PHRASES;
		}

		return phrases.toArray(new String[phrases.size()]);
	}

	/**
	 * Converte:
	 *
	 * X,Y,Z;X,Y,Z;X,Y,Z
	 *
	 * em uma rota.
	 */
	private Location[] parseRoute(String value)
	{
		String[] points = value.split(";");

		List<Location> locations = new ArrayList<Location>();

		for (String point : points)
		{
			String[] coordinates = point.trim().split(",");

			if (coordinates.length < 3)
			{
				continue;
			}

			try
			{
				int x = Integer.parseInt(coordinates[0].trim());
				int y = Integer.parseInt(coordinates[1].trim());
				int z = Integer.parseInt(coordinates[2].trim());

				locations.add(
					new Location(x, y, z)
				);
			}
			catch (NumberFormatException e)
			{
				/*
				 * Ignora somente o ponto inválido.
				 */
			}
		}

		return locations.toArray(
			new Location[locations.size()]
		);
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();

		_routeIndex = 0;

		/*
		 * Primeira fala.
		 */
		if (_phrases.length > 0)
		{
			addTimer(
				TALK_TIMER,
				_firstTalk
			);
		}

		/*
		 * Só inicia caminhada se houver rota.
		 */
		if (_route.length > 0)
		{
			addTimer(
				WALK_TIMER,
				_walkStart
			);
		}
	}

	@Override
	protected void onEvtArrived()
	{
		super.onEvtArrived();

		if (_route.length == 0)
		{
			return;
		}

		/*
		 * Avança para o próximo ponto.
		 */
		_routeIndex++;

		if (_routeIndex >= _route.length)
		{
			_routeIndex = 0;
		}

		/*
		 * Pausa antes de continuar.
		 */
		addTimer(
			WALK_TIMER,
			_walkPause
		);
	}

	@Override
	protected void onEvtArrivedBlocked(Location blockedAt)
	{
		super.onEvtArrivedBlocked(blockedAt);

		if (_route.length == 0)
		{
			return;
		}

		/*
		 * Tenta novamente caso o caminho esteja bloqueado.
		 */
		addTimer(
			WALK_TIMER,
			_walkBlocked
		);
	}

	@Override
	protected void onEvtTimer(
		int timerId,
		Object arg1,
		Object arg2)
	{
		super.onEvtTimer(
			timerId,
			arg1,
			arg2
		);

		Creature actor = getActor();

		if (actor == null || actor.isDead())
		{
			return;
		}

		/*
		 * =====================================================
		 * FALA
		 * =====================================================
		 */
		if (timerId == TALK_TIMER)
		{
			if (actor instanceof NpcInstance &&
				_phrases.length > 0)
			{
				NpcInstance npc =
					(NpcInstance) actor;

				String phrase =
					_phrases[
						(int) (
							Math.random() *
							_phrases.length
						)
					];

				npc.broadcastPacket(
					new Say2(
						npc,
						ChatType.NPC_ALL,
						phrase
					)
				);
			}

			/*
			 * Próxima fala.
			 */
			long nextTalk = _talkMin;

			if (_talkMax > _talkMin)
			{
				nextTalk =
					_talkMin +
					(long) (
						Math.random() *
						(_talkMax - _talkMin)
					);
			}

			addTimer(
				TALK_TIMER,
				nextTalk
			);

			return;
		}

		/*
		 * =====================================================
		 * CAMINHADA
		 * =====================================================
		 */
		if (timerId == WALK_TIMER)
		{
			if (!(actor instanceof NpcInstance))
			{
				return;
			}

			if (_route.length == 0)
			{
				return;
			}

			NpcInstance npc =
				(NpcInstance) actor;

			Location destination =
				_route[_routeIndex];

			npc.moveToLocation(
				destination,
				0,
				true
			);
		}
	}
}
