package ctfbot;

public interface IBehavior {

	boolean isFiring();
	void run();
	boolean terminate();
	int getPriority();
	
}
