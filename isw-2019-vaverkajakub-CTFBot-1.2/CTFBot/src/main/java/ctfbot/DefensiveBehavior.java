package ctfbot;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.utils.Cooldown;

public class DefensiveBehavior implements IBehavior {

	
	private CTFBot bot = null;

	public DefensiveBehavior(CTFBot ctfBot) {
		this.bot = ctfBot;
	}
	
	@Override
	public void run() {
		UnrealId target = bot.getGameState().getOurFlagHolder();
		if (target != null) {
			Player targetPlayer = bot.getPlayers().getPlayer(target);
			if (targetPlayer != null && targetPlayer.getLocation() != null) {
				bot.goTo(targetPlayer.getLocation());
			}
			bot.shoot(targetPlayer);
		} else if (bot.getGameState().getOurFlagLocation() != null
				&& bot.getGameState().getOurFlagLocation() != bot.getCTF().getOurBase().getLocation()) {
			bot.goTo(bot.getGameState().getOurFlagLocation());
		} else {
			bot.goTo(bot.getGameState().getEnemyFlagLocation());
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
		if(!bot.getGameState().isOurFlagSafe()) {
			return 200;
		}
		return 0;
	}

}
