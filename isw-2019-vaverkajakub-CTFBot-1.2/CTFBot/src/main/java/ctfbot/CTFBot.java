package ctfbot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import ctfbot.tc.CTFCommItems;
import ctfbot.tc.CTFCommObjectUpdates;
import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import cz.cuni.amis.pathfinding.map.IPFMapView;
import cz.cuni.amis.pogamut.base.agent.impl.AgentId;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.impl.PrecomputedPathFuture;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassEventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.object.event.WorldObjectUpdatedEvent;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils.GetLocatedDistance3D;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.Rotation;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.NavigationGraphBuilder;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.UT2004Skins;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004MapTweaks;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.levelGeometry.RayCastResult;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.NavMeshClearanceComputer.ClearanceLimit;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.pathfollowing.NavMeshNavigation;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.bot.params.UT2004BotParameters;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.FlagInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.HearNoise;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.IncomingProjectile;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ItemPickedUp;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.TeamScore;
import cz.cuni.amis.pogamut.ut2004.teamcomm.bot.UT2004BotTCController;
import cz.cuni.amis.pogamut.ut2004.teamcomm.server.UT2004TCServer;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.ExceptionToString;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import math.geom2d.Vector2D;

/**
 * CTF BOT TEMPLATE CLASS
 * Version: 0.0.1
 */
@AgentScoped
public class CTFBot extends UT2004BotTCController<UT2004Bot> {

	private static Object CLASS_MUTEX = new Object();
	
	/**
	 * How many bots to start...
	 */
	public static int BOTS_TO_START = 6;
	public static int team = -1;
	public static int teamSize = -1;
	public static int skill = 7;
	public static String host = "127.0.0.1";
	
	/**
	 * TRUE => attempt to auto-load level geometry on bot startup
	 */
	public static final boolean LOAD_LEVEL_GEOMETRY = false;
	
	/**
	 * TRUE => draws navmesh and terminates
	 */
	public static final boolean DRAW_NAVMESH = false;
	private static boolean navmeshDrawn = false;
	
	/**
	 * TRUE => rebinds NAVMESH+NAVIGATION GRAPH; useful when you add new map tweak into {@link MapTweaks}.
	 */
	public static final boolean UPDATE_NAVMESH = true;
	
	/**
	 * Whether to draw navigation path; works only if you are running 1 bot...
	 */
	public static final boolean DRAW_NAVIGATION_PATH = false;
	boolean navigationPathDrawn = false;
	
	/**
	 * If true, all bots will enter RED team... 
	 */
	public static final boolean START_BOTS_IN_SINGLE_TEAM = false;
	
	
	public BehaviorManager behaviorManager = null;
		
	/**
	 * How many bots we have started so far; used to split bots into teams.
	 */
	private static AtomicInteger BOT_COUNT = new AtomicInteger(0);
	/**
	 * How many bots have entered RED team.
	 */
	private static AtomicInteger BOT_COUNT_RED_TEAM = new AtomicInteger(0);
	/**
	 * How many bots have entered BLUE team.
	 */
	private static AtomicInteger BOT_COUNT_BLUE_TEAM = new AtomicInteger(0);
	
	/**
	 * 0-based; note that during the tournament all your bots will have botInstance == 0!
	 */
	private int botInstance = 0;
	
	/**
	 * 0-based; note that during the tournament all your bots will have botTeamInstance == 0!
	 */
	private int botTeamInstance = 0;
	
	private CTFCommItems<CTFBot> commItems;
	private CTFCommObjectUpdates<CTFBot> commObjectUpdates;

	private GameState gameState;
	private TabooSet<Item> forbiddenItems;
	
	private GetLocatedDistance3D<ILocated> distance3D = new GetLocatedDistance3D<ILocated>();

    // =============
    // BOT LIFECYCLE
    // =============
    
    public GetLocatedDistance3D<ILocated> getDistance3D() {
		return distance3D;
	}

	public void setDistance3D(GetLocatedDistance3D<ILocated> distance3d) {
		distance3D = distance3d;
	}

	/**
     * Bot's preparation - called before the bot is connected to GB2004 and launched into UT2004.
     */
    @Override
    public void prepareBot(UT2004Bot bot) {       	
        // DEFINE WEAPON PREFERENCES
        initWeaponPreferences();
        
        // INITIALIZATION OF COMM MODULES
        commItems = new CTFCommItems<CTFBot>(this);
        commObjectUpdates = new CTFCommObjectUpdates<CTFBot>(this);
    }
    
    @Override
    protected void initializeModules(UT2004Bot bot) {
    	super.initializeModules(bot);
    	levelGeometryModule.setAutoLoad(LOAD_LEVEL_GEOMETRY);
    }
    
    /**
     * This is a place where you should use map tweaks, i.e., patch original Navigation Graph that comes from UT2004.
     */
    @Override
    public void mapInfoObtained() {
    	// See {@link MapTweaks} for details; add tweaks in there if required.
    	 
        MapTweaks.tweak(navBuilder);    	
   		navMeshModule.setReloadNavMesh(UPDATE_NAVMESH);  
    	
      
    }
    
    /**
     * Define your weapon preferences here (if you are going to use weaponPrefs).
     * 
     * For more info, see slides (page 8): http://diana.ms.mff.cuni.cz/pogamut_files/lectures/2010-2011/Pogamut3_Lecture_03.pdf
     */
    private void initWeaponPreferences() {
    	weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);
		
		getWeaponPrefs().newPrefsRange(100).add(UT2004ItemType.SHIELD_GUN, true).add(UT2004ItemType.FLAK_CANNON, true)
				.add(UT2004ItemType.LINK_GUN, false).add(UT2004ItemType.MINIGUN, true)
				.add(UT2004ItemType.BIO_RIFLE, true);
// < 2.5m
		getWeaponPrefs().newPrefsRange(250).add(UT2004ItemType.FLAK_CANNON, true).add(UT2004ItemType.LINK_GUN, false)
				.add(UT2004ItemType.MINIGUN, true).add(UT2004ItemType.BIO_RIFLE, true);

// < 5m
		getWeaponPrefs().newPrefsRange(500).add(UT2004ItemType.MINIGUN, true).add(UT2004ItemType.FLAK_CANNON, true)
				.add(UT2004ItemType.LINK_GUN, false).add(UT2004ItemType.BIO_RIFLE, true);

// < 10m
		getWeaponPrefs().newPrefsRange(1000).add(UT2004ItemType.MINIGUN, true).add(UT2004ItemType.LINK_GUN, true)
				.add(UT2004ItemType.SHOCK_RIFLE, true).add(UT2004ItemType.ROCKET_LAUNCHER, true);

// < 25m
		getWeaponPrefs().newPrefsRange(2500).add(UT2004ItemType.SHOCK_RIFLE, true).add(UT2004ItemType.MINIGUN, false);
	}

    @Override
    public Initialize getInitializeCommand() {
    	// IT IS FORBIDDEN BY COMPETITION RULES TO CHANGE DESIRED SKILL TO DIFFERENT NUMBER THAN 6
    	// IT IS FORBIDDEN BY COMPETITION RULES TO ALTER ANYTHING EXCEPT NAME & SKIN VIA INITIALIZE COMMAND
		// Jakub Gemrot -> targetName = "JakubGemrot"
		String targetName = "JakubVaverka";
		botInstance = BOT_COUNT.getAndIncrement();

		if (team == -1) {
			int targetTeam = AgentInfo.TEAM_RED;
			if (!START_BOTS_IN_SINGLE_TEAM) {
				targetTeam = botInstance % 2 == 0 ? AgentInfo.TEAM_RED : AgentInfo.TEAM_BLUE;
			}
			switch (targetTeam) {
			case AgentInfo.TEAM_RED:
				botTeamInstance = BOT_COUNT_RED_TEAM.getAndIncrement();
				targetName += "-RED-" + botTeamInstance;
				break;
			case AgentInfo.TEAM_BLUE:
				botTeamInstance = BOT_COUNT_BLUE_TEAM.getAndIncrement();
				targetName += "-BLUE-" + botTeamInstance;
				break;
			}
			return new Initialize().setName(targetName).setSkin(targetTeam == AgentInfo.TEAM_RED ? UT2004Skins.SKINS[0]
					: UT2004Skins.SKINS[UT2004Skins.SKINS.length - 1]).setTeam(targetTeam).setDesiredSkill(6);

		}else {
			return new Initialize().setName(targetName).setSkin(team == AgentInfo.TEAM_RED ? UT2004Skins.SKINS[0]
					: UT2004Skins.SKINS[UT2004Skins.SKINS.length - 1]).setTeam(team).setDesiredSkill(skill);
		}
	}

    /**
     * Bot has been initialized inside GameBots2004 (Unreal Tournament 2004) and is about to enter the play
     * (it does not have the body materialized yet).
     *  
     * @param gameInfo
     * @param currentConfig
     * @param init
     */
    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
    	behaviorManager = new BehaviorManager(this);
    	gameState = new GameState(this);
    }

    // ==========================
    // EVENT LISTENERS / HANDLERS
    // ==========================
	
    /**
     * {@link PlayerDamaged} listener that senses that "some other bot was hurt".
     *
     * @param event
     */
    @EventListener(eventClass = PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
    	UnrealId botHurtId = event.getId();
    	if (botHurtId == null) return;
    	
    	int damage = event.getDamage();
   // 	Player botHurt = (Player)world.get(botHurtId); // MAY BE NULL!
    	
    	log.info("OTHER HURT: " + damage + " DMG to " + botHurtId.getStringId() + " [type=" + event.getDamageType() + ", weapon=" + event.getWeaponName() + "]");
    }
    
 
    /**
     * {@link BotDamaged} listener that senses that "I was hurt".
     *
     * @param event
     */
    @EventListener(eventClass = BotDamaged.class)
    public void botDamaged(BotDamaged event) {
    	int damage = event.getDamage();
    	
    	if (event.getInstigator() == null) {
    		log.info("HURT: " + damage + " DMG done to ME [type=" + event.getDamageType() + ", weapon=" + event.getWeaponName() + "] by UNKNOWN");
    	} else {
    		UnrealId whoCauseDmgId = event.getInstigator();
  //  		Player player = (Player) world.get(whoCauseDmgId); // MAY BE NULL!
    		log.info("HURT: " + damage + " DMG done to ME [type=" + event.getDamageType() + ", weapon=" + event.getWeaponName() + "] by " + whoCauseDmgId.getStringId());
    	}
    }
    
    /**
     * {@link PlayerKilled} listener that senses that "some other bot has died".
     *
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
    	UnrealId botDiedId = event.getId();
    	if (botDiedId == null) return;
    	
  //  	Player botDied = (Player) world.get(botDiedId);
    	
    	if (event.getKiller() == null) {
    		log.info("OTHER DIED: " + botDiedId.getStringId() + ", UNKNOWN killer");
    	} else {
    		UnrealId killerId = event.getKiller();
    		if (killerId.equals(info.getId())) {
    			log.info("OTHER KILLED: " + botDiedId.getStringId() + " by ME");
    		} else {
    //			Player killer = (Player) world.get(killerId);
    			if (botDiedId.equals(killerId)) {
    				log.info("OTHER WAS KILLED: " + botDiedId.getStringId() + " comitted suicide");
    			} else {
    				log.info("OTHER WAS KILLED: " + botDiedId.getStringId() + " by " + killerId.getStringId());
    			}
    		}
    	}
    }
    
    /**
     * {@link BotKilled} listener that senses that "your bot has died".
     */
	@Override
	public void botKilled(BotKilled event) {
		if (event.getKiller() == null) {
			log.info("DEAD");
		} else {
			UnrealId killerId = event.getKiller();
//			Player killer = (Player) world.get(killerId);
			log.info("KILLED by" + killerId.getStringId());
		} 
	}
	
    /**
     * {@link HearNoise} listener that senses that "some noise was heard by the bot".
     *
     * @param event
     */
    @EventListener(eventClass = HearNoise.class)
    public void hearNoise(HearNoise event) {
    	double noiseDistance = event.getDistance();   // 100 ~ 1 meter
 //   	Rotation faceRotation = event.getRotation();  // rotate bot to this if you want to face the location of the noise
    	log.info("HEAR NOISE: distance = " + noiseDistance);
    }
    
    /**
     * {@link ItemPickedUp} listener that senses that "your bot has picked up some item".
     * 
     * See sources for {@link ItemType} for details about item types / categories / groups.
     *
     * @param event
     */
    @EventListener(eventClass = ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
    	ItemType itemType = event.getType();
    	ItemType.Group itemGroup = itemType.getGroup();
    	ItemType.Category itemCategory = itemType.getCategory();
    	log.info("PICKED " + itemCategory.name + ": " + itemType.getName() + " [group=" + itemGroup.getName() + "]");    	
    }
    
    /**
     * {@link IncomingProjectile} listener that senses that "some projectile has appeared OR moved OR disappeared".
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = IncomingProjectile.class, eventClass = WorldObjectUpdatedEvent.class)
    public void incomingProjectileUpdated(WorldObjectUpdatedEvent<IncomingProjectile> event) {
    	IncomingProjectile projectile = event.getObject();
    	log.info("PROJECTILE UPDATED: " + projectile);
    }
    
    /**
     * {@link Player} listener that senses that "some other bot has appeared OR moved OR disappeared"
     *
     * WARNING: this method will also be called during handshaking GB2004.
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = Player.class, eventClass = WorldObjectUpdatedEvent.class)
    public void playerUpdated(WorldObjectUpdatedEvent<Player> event) {
    	if (info.getLocation() == null) {
    		// HANDSHAKING GB2004
    		return;
    	}
    	Player player = event.getObject();
    	gameState.updatePlayer(player);
    	log.info("PLAYER UPDATED: " + player.getId().getStringId());
    }
        
    /**
     * {@link Item} listener that senses that "some SPAWNED item has appeared OR moved OR disappeared"
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = Item.class, eventClass = WorldObjectUpdatedEvent.class)
    public void itemUpdated(WorldObjectUpdatedEvent<Item> event) {
    	if (info.getLocation() == null) {
    		// HANDSHAKING GB2004
    		return;
    	}
    	Item item = event.getObject();
    	log.info("ITEM UPDATED: " + item.getId().getStringId());
    }
    
    /**
     * {@link FlagInfo} listener that senses changes of CTF game state.
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = FlagInfo.class, eventClass = WorldObjectUpdatedEvent.class)
    public void flagInfoUpdated(WorldObjectUpdatedEvent<FlagInfo> event) {
    	log.info("FLAG INFO UPDATED: " + event.getObject());
    	 FlagInfo flag = (FlagInfo)event.getObject();
         log.info("FLAG STATE CHANGE: " + flag.getLocation() + " " +flag.getHolder() + " " + flag.getState());
         if(flag.getTeam() == getInfo().getTeam()){//OURS
        	 getGameState().setOurFlag(flag);
			if(flag.getState().equals("home")){
                 getGameState().setOurFlagLocation(ctf.getOurBase().getLocation());
                 getGameState().setOurFlagSafe(true);
                 getGameState().setOurFlagHolder(flag.getHolder());
                 return;
             }
             if(flag.getState().equals("dropped")){
            	 getGameState().setOurFlagSafe(false);
            	 getGameState().setOurFlagHolder(null);
             }
             if(flag.getLocation() != null){
                 if(flag.getHolder() != null){
                	 getGameState().setOurFlagSafe(false);
                	 getGameState().setOurFlagHolder(flag.getHolder());
                 }
                 if(getGameState().getOurFlagLocation() == flag.getLocation()){
                	 return;
                 }
                 getGameState().setOurFlagLocation(flag.getLocation());
             }
         }else{//ENEMYS
        	 getGameState().setEnemyFlag(flag);
             if(flag.getState().equals("home")){
                 getGameState().setEnemyFlagLocation(ctf.getEnemyBase().getLocation());
                 return;
             } else if(flag.getLocation() != null){
                 if(getGameState().getEnemyFlagLocation() == flag.getLocation()){
                     return;
                 }
                 getGameState().setEnemyFlagLocation(flag.getLocation());
             }
         }
    }
    
    /**
     * {@link TeamScore} listener that senses changes within scoring.
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = TeamScore.class, eventClass = WorldObjectUpdatedEvent.class)
    public void teamScoreUpdated(WorldObjectUpdatedEvent<TeamScore> event) {
    	switch (event.getObject().getTeam()) {
    	case AgentInfo.TEAM_RED: 
    		log.info("RED TEAM SCORE UPDATED: " + event.getObject());
    		break;
    	case AgentInfo.TEAM_BLUE:
    		log.info("BLUE TEAM SCORE UPDATED: " + event.getObject());
    		break;
    	}
    }
    
    
    private long selfLastUpdateStartMillis = 0;
    private long selfTimeDelta = 0;
    
    /**
     * {@link Self} object has been updated. This update is received about every 50ms. You can use this update
     * to fine-time some of your behavior like "weapon switching". I.e. SELF is updated every 50ms while LOGIC is invoked every 250ms.
     * 
     * Note that during "SELF UPDATE" only information about your bot location/rotation ({@link Self}) is updated. All other visibilities 
     * remains the same as during last {@link #logic()}.
     * 
     * Note that new {@link NavMeshNavigation} is using SELF UPDATES to fine-control the bot's navigation.
     * 
     * @param event
     */
    @ObjectClassEventListener(objectClass = Self.class, eventClass = WorldObjectUpdatedEvent.class)
    public void selfUpdated(WorldObjectUpdatedEvent<Self> event) {
    	if (lastLogicStartMillis == 0) {
    		// IGNORE ... logic has not been executed yet...
    		return;
    	}
    	if (selfLastUpdateStartMillis == 0) {
    		selfLastUpdateStartMillis = System.currentTimeMillis();
    		return;
    	}
    	long selfUpdateStartMillis = System.currentTimeMillis(); 
    	selfTimeDelta = selfUpdateStartMillis  - selfLastUpdateStartMillis;
    	selfLastUpdateStartMillis = selfUpdateStartMillis;
    	log.info("---[ SELF UPDATE | D: " + (selfTimeDelta) + "ms ]---");
    	
    	try {
    		
    		// YOUR CODE HERE
    		
    	} catch (Exception e) {
    		// MAKE SURE THAT YOUR BOT WON'T FAIL!
    		log.info(ExceptionToString.process(e));
    	} finally {
    		//log.info("---[ SELF UPDATE END ]---");
    	}
    	
    }

    // ==============
    // MAIN BOT LOGIC
    // ==============

    Item targetItem = null;
    int targetItemStuck = 0;
    /**
     * Method that is executed only once before the first {@link CTFBot#logic()} 
     */
    @Override
    public void beforeFirstLogic() {
    	forbiddenItems = new TabooSet<Item>(bot);
    	this.navigation.addStrongNavigationListener(
      		   new FlagListener<NavigationState>() {
      		      @Override
      		      public void flagChanged(NavigationState changedValue){
      		          switch (changedValue) {
      		              case STUCK:
      		            	  log.info("STUCK!");
      		            	  ++targetItemStuck;
      		            	  if (targetItemStuck >= 3) {
      		            		 log.info("TABOO [stuck]: " + targetItem.getId() + " / " + targetItem.getType());
     		            		  getForbiddenItems().add(targetItem, 10);
      		            	  }
      		                  break;
      		              case STOPPED:
      		            	  log.info("STOPPED!");
      		                  break;
      		              case TARGET_REACHED:
      		            	  log.info("TARGET REACHED!");
      		            	  targetItemStuck = 0;
      		                  break;
      		              case PATH_COMPUTATION_FAILED:
      		            	  log.info("FAILED TO COMPUTE PATH!");
      		            	  if (targetItem != null) {
      		            		  log.info("TABOO [nopath]: " + targetItem.getId() + " / " + targetItem.getType());
      		            		  getForbiddenItems().add(targetItem, 10);
      		            	  }
      		                  break;                      
      		              case NAVIGATING:
      		            	  log.info("Navigation continues...");
      		                  break;
      		          }
      		      }
      		   });
    	
    	gameState.setPossibleItemsCounts(getItems().getSpawnedItems().values().stream().filter(
 		    	(item) -> {
 		    		return getItems().isPickable(item) && !getForbiddenItems().isTaboo(item);
 		    	}
 	        ).collect(Collectors.toList()).size());
    	
    }
    
    private long lastLogicStartMillis = 0;
    private long lastLogicEndMillis = 0;
    private long timeDelta = 0;
    
    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second.
     * 
     * This is a typical place from where you start coding your bot. Even though bot
     * can be completely EVENT-DRIVEN, the reactive aproach via "ticking" logic()
     * method is more simple / straight-forward.
     */
    @Override
    public void logic() {
    	long logicStartTime = System.currentTimeMillis();
    	if (lastLogicStartMillis == 0) {
    		lastLogicStartMillis = logicStartTime;
    		log.info("===[ LOGIC ITERATION ]===");
    		timeDelta = 1;
    	} else {
    		timeDelta = logicStartTime - lastLogicStartMillis;
    		log.info("===[ LOGIC ITERATION | Delta: " + (timeDelta) + "ms | Since last: " + (logicStartTime - lastLogicEndMillis) + "ms]===");    		
    		lastLogicStartMillis = logicStartTime;
    	}
    	
    	if (DRAW_NAVMESH && botInstance == 0) {
    		boolean drawNavmesh = false;
    		synchronized(CLASS_MUTEX) {
    			if (!navmeshDrawn) {
    				drawNavmesh = true;
    				navmeshDrawn = true;
    			}
    		}
    		if (drawNavmesh) {
    			log.warning("!!! DRAWING NAVMESH !!!");
    			navMeshModule.getNavMeshDraw().draw(true, true);
    			navmeshDrawn  = true;
    			log.warning("NavMesh drawn, waiting a bit to finish the drawing...");
    		}    		
    	}
    	
    	drawState();
    	
    	try {
	    	// LOG VARIOUS INTERESTING VALUES
	    	logMind();
	    	
	    	// UPDATE TEAM COMM
	    	commItems.update();
	    	commObjectUpdates.update();
	    	
	    	behaviorManager.suggestBehavior();
	    	
	    	
    	} catch (Exception e) {
    		// MAKE SURE THAT YOUR BOT WON'T FAIL!
    		log.info(ExceptionToString.process(e));
    	} finally {
    		// MAKE SURE THAT YOUR LOGIC DOES NOT TAKE MORE THAN 250 MS (Honestly, we have never seen anybody reaching even 150 ms per logic cycle...)
    		// Note that it is perfectly OK, for instance, to count all path-distances between you and all possible pickup-points / items in the game
    		// sort it and do some inference based on that.
    		long timeSpentInLogic = System.currentTimeMillis() - logicStartTime;
    		log.info("Logic time:         " + timeSpentInLogic + " ms");
    		if (timeSpentInLogic >= 245) {
    			log.warning("!!! LOGIC TOO DEMANDING !!!");
    		}
    		log.info("===[ LOGIC END ]===");
    		lastLogicEndMillis = System.currentTimeMillis();
    	}    	
    }
    
    // ===========
    // MIND LOGGER
    // ===========
    
    /**
     * It is good in-general to periodically log anything that relates to your's {@link CTFBot#logic()} decision making.
     * 
     * You might consider exporting these values to some custom Swing window (you crete for yourself) that will be more readable.
     */
    public void logMind() {
    	log.info("My health/armor:   " + info.getHealth() + " / " + info.getArmor() + " (low:" + info.getLowArmor() + " / high:" + info.getHighArmor() + ")");
    	log.info("My weapon:         " + weaponry.getCurrentWeapon());
    	log.info("Have flag:         " + ctf.isBotCarryingEnemyFlag());
    	log.info("Our flag state:    " + ctf.getOurFlag().getState());
    	log.info("Enemey flag state: " + ctf.getEnemyFlag().getState());
    }
    
    // ======================================
    // UT2004 DEATH-MATCH INTERESTING GETTERS
    // ======================================
    
    /**
     * Returns path-nearest {@link NavPoint} that is covered from 'enemy'. Uses {@link UT2004BotModuleController#getVisibility()}.
     * @param enemy
     * @return
     */
    public NavPoint getNearestCoverPoint(Player enemy) {
    	if (!visibility.isInitialized()) {
    		log.warning("VISIBILITY NOT INITIALIZED: returning random navpoint");    		
    		return MyCollections.getRandom(navPoints.getNavPoints().values());
    	}
    	List<NavPoint> coverPoints = new ArrayList<NavPoint>(visibility.getCoverNavPointsFrom(enemy.getLocation()));
    	return fwMap.getNearestNavPoint(coverPoints, info.getNearestNavPoint());
    }
    
    /**
     * Returns whether 'item' is possibly spawned (to your current knowledge).
     * @param item
     * @return
     */
    public boolean isPossiblySpawned(Item item) {
    	return items.isPickupSpawned(item);
    }
    
    /**
     * Returns whether you can actually pick this 'item', based on "isSpawned" and "isPickable" in your current state and knowledge.
     */
    public boolean isCurrentlyPickable(Item item) {
    	return isPossiblySpawned(item) && items.isPickable(item);
    }
        
    // ==========
    // RAYCASTING
    // ==========
    
    /**
     * Performs a client-side raycast agains UT2004 map geometry.
     * 
     * It is not sensible to perform more than 1000 raycasts per logic() per bot.
     *  
     * @param from
     * @param to
     * @return
     */
	public RayCastResult raycast(ILocated from, ILocated to) {
		if (!levelGeometryModule.isInitialized())
			return null;
		return levelGeometryModule.getLevelGeometry().rayCast(from.getLocation(), to.getLocation());
	}
    
    /**
     * Performs a client-side raycast against NavMesh in 'direction'. Returns distance of the edge in given 'direction' sending the ray 'from'.
     * @param from
     * @param direction
     * @return
     */
	public double raycastNavMesh(ILocated from, Vector2D direction) {
		if (!navMeshModule.isInitialized())
			return 0;
		ClearanceLimit limit = navMeshModule.getClearanceComputer().findEdge(from.getLocation(), direction);
		if (limit == null)
			return Double.POSITIVE_INFINITY;
		return from.getLocation().getDistance(limit.getLocation());
	}
    
    // =======
    // DRAWING
    // =======
    public void drawState() {
    	draw.clearAll();
    	IBehavior current = behaviorManager.getCurrentBehavior();
    	if(current instanceof StuckBehavior) {
    		draw.drawLine(Color.MAGENTA, bot.getLocation(),bot.getLocation().addZ(100));    		
    	}else if(current instanceof CaptureBehavior) {
    		draw.drawLine(Color.GREEN, bot.getLocation(),bot.getLocation().addZ(100)); 
    	}else if(current instanceof FightBehavior) {
    		draw.drawLine(Color.RED, bot.getLocation(),bot.getLocation().addZ(100)); 
    	}else if(current instanceof CollectBehavior) {
    		draw.drawLine(Color.YELLOW, bot.getLocation(),bot.getLocation().addZ(100)); 
    	}else if(current instanceof DefensiveBehavior) {
    		draw.drawLine(Color.GRAY, bot.getLocation(),bot.getLocation().addZ(100)); 
    	}else {
    		draw.drawLine(Color.CYAN, bot.getLocation(),bot.getLocation().addZ(100)); 
    	}
    }
	
    public void drawNavigationPath(boolean clearAll) {
    	if (clearAll) {
    		draw.clearAll();
    	}
    	List<ILocated> path = navigation.getCurrentPathCopy();
    	for (int i = 1; i < path.size(); ++i) {
    		draw.drawLine(path.get(i-1), path.get(i));
    	}
    }
    
    public void drawPath(IPathFuture<? extends ILocated> pathFuture, boolean clearAll) {
    	if (clearAll) {
    		draw.clearAll();
    	}
    	List<? extends ILocated> path = pathFuture.get();
    	for (int i = 1; i < path.size(); ++i) {
    		draw.drawLine(path.get(i-1), path.get(i));
    	}
    }
    
    public void drawPath(IPathFuture<? extends ILocated> pathFuture, Color color, boolean clearAll) {
    	if (clearAll) {
    		draw.clearAll();
    	}
    	if (color == null) color = Color.WHITE;
    	List<? extends ILocated> path = pathFuture.get();
    	for (int i = 1; i < path.size(); ++i) {
    		draw.drawLine(color, path.get(i-1), path.get(i));
    	}
    }
    
    // =====
    // AStar
    // =====
    
    private NavPoint lastAStarTarget = null;
    
    public boolean navigateAStarPath(NavPoint targetNavPoint) {
        if (lastAStarTarget == targetNavPoint) {
            if (navigation.isNavigating()) return true;
        }
        PrecomputedPathFuture<ILocated> path = getAStarPath(targetNavPoint);
        if (path == null) {
            navigation.stopNavigation();
            return false;
        }
        lastAStarTarget = targetNavPoint;
        navigation.navigate(path);
        return true;
    }
    
    private IPFMapView<NavPoint> mapView = new IPFMapView<NavPoint>() {

        @Override
        public Collection<NavPoint> getExtraNeighbors(NavPoint node, Collection<NavPoint> mapNeighbors) {
            return null;
        }

        @Override
        public int getNodeExtraCost(NavPoint node, int mapCost) {
            return 0;
        }

        @Override
        public int getArcExtraCost(NavPoint nodeFrom, NavPoint nodeTo, int mapCost) {
            return 0;
        }

        @Override
        public boolean isNodeOpened(NavPoint node) {
            return true;
        }

        @Override
        public boolean isArcOpened(NavPoint nodeFrom, NavPoint nodeTo) {
            return true;
        }
    };
    
    private PrecomputedPathFuture<ILocated> getAStarPath(NavPoint targetNavPoint) {
        NavPoint startNavPoint = info.getNearestNavPoint();
        AStarResult<NavPoint> result = aStar.findPath(startNavPoint, targetNavPoint, mapView);
        if (result == null || !result.isSuccess()) return null;
        PrecomputedPathFuture path = new PrecomputedPathFuture(startNavPoint, targetNavPoint, result.getPath());
     
        return path;
    }
    
    // ===========
    // MAIN METHOD
    // ===========
    
    /**
     * Main execute method of the program.
     * 
     * @param args
     * @throws PogamutException
     */
    public static void main(String args[]) throws PogamutException {
    	if(args.length>1) {
	    	team = Integer.valueOf(args[1]);
			skill = Integer.valueOf(args[2]);
			teamSize = Integer.valueOf(args[3]);
			host = args[4];
			BOTS_TO_START = teamSize;
    	}
    	UT2004TCServer tcServer = UT2004TCServer.startTCServer();
    	
		// setup for 3 - 8 bots
		
    	// Starts N agents of the same type at once
    	// WHEN YOU WILL BE SUBMITTING YOUR CODE, MAKE SURE THAT YOU RESET NUMBER OF STARTED AGENTS TO '1' !!!
    	new UT2004BotRunner(CTFBot.class, "CTFBot").setMain(true).setHost(host).setLogLevel(Level.SEVERE).startAgents(BOTS_TO_START);
    }
    

    public void goTo(ILocated target) {
		if (target == null) {
			log.warning("TRYING TO NAVIGATE TO NULL TARGET");
			navigation.stopNavigation();
			return;
		}
		Location pathTarget = target.getLocation();
        if(pathTarget == null){
            log.warning("LOCATION OF TARGET IS NULL");
            navigation.stopNavigation();
        }
       
        	navigation.navigate(target);        	
        	getGameState().setCurrentNavigationTarget(target.getLocation());
        	log.info("GOING TO LOCATION " +target.getLocation() + " TO GET " +target);
        
        
	}
    
	private Cooldown lightCD = new Cooldown(2500);
	
    public void shoot(Player target) {
		if (lightCD.tryUse() && getWeaponry().hasWeapon(UT2004ItemType.LIGHTNING_GUN)) {
			getShoot().shoot(getWeaponPrefs(), target);
		} else {
			getShoot().shoot(getWeaponPrefs(), target, UT2004ItemType.LIGHTNING_GUN);
		}
	}

    public void doubleJumpTo(ILocated target) {
    	ILocated runningTo = navigation.getCurrentTarget();
    	if(runningTo != null) {
    		Location dodgePoint = runningTo.getLocation();
    		if(dodgePoint != null) {
    			move.dodge(dodgePoint, true);    
    		}
    	}  
    }
    
	public GameState getGameState() {
		return gameState;
	}

	public TabooSet<Item> getForbiddenItems() {
		return forbiddenItems;
	}

	public void setForbiddenItems(TabooSet<Item> forbiddenItems) {
		this.forbiddenItems = forbiddenItems;
	}

	

}
