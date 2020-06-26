package ctfbot;

import java.util.ArrayList;
import java.util.Comparator;

public class BehaviorManager {
	
	private ArrayList<IBehavior> behaviors = new ArrayList<IBehavior>();
	private IBehavior currentBehavior = null;

	public BehaviorManager(CTFBot ctfBot) {
		behaviors.add(new StuckBehavior(ctfBot));
		behaviors.add(new CaptureBehavior(ctfBot));
		behaviors.add(new FightBehavior(ctfBot));
		behaviors.add(new CollectBehavior(ctfBot));
		behaviors.add(new DefensiveBehavior(ctfBot));
	}
	
	public void suggestBehavior() {
		behaviors.sort(new Comparator<IBehavior>() {

			@Override
			public int compare(IBehavior o1, IBehavior o2) {
				if(o1.getPriority()>o2.getPriority()) {
					return -1;
				}else if(o1.getPriority()<o2.getPriority()) {
					return 1;
				}
				return 0;
			}
		});
		currentBehavior = behaviors.get(0);
		currentBehavior.run();
	}
	
	public IBehavior getCurrentBehavior() {
		return currentBehavior;
	}
	
	
}
