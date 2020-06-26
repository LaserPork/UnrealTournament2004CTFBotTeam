package ctfbot;

import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.utils.Cooldown;

public class FightBehavior implements IBehavior {

	
	private CTFBot bot = null;


	public FightBehavior(CTFBot ctfBot) {
		this.bot = ctfBot;
	}
	
	@Override
	public void run() {
    	Player target = bot.getPlayers().getNearestEnemy(3000);
    	if(target != null) {
    		bot.goTo(target.getLocation());
    		bot.shoot(target);    		
    	}
	}
	
	

	@Override
	public boolean isFiring() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getPriority() {
		Player enemy = bot.getPlayers().getNearestEnemy(3000);
		double distance = 2000;
		if(enemy != null) {
			distance = bot.getDistance3D().getDistance(bot.getBot().getLocation(), enemy.getLocation());
		//	bot.get
		}
		return (int)(200 - distance/10);
	}

}
