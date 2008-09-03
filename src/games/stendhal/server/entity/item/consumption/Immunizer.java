package games.stendhal.server.entity.item.consumption;

import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.core.events.TurnListener;
import games.stendhal.server.core.events.TurnNotifier;
import games.stendhal.server.entity.item.ConsumableItem;
import games.stendhal.server.entity.player.Player;

class Immunizer implements Feeder {

	public boolean feed(final ConsumableItem item, final Player player) {
		player.setImmune();
		
		// set a timer to remove the immunity effect after some time
		final TurnNotifier notifier = SingletonRepository.getTurnNotifier();
		// first remove all effects from previously used immunities to
		// restart the timer
		
		final TurnListener tl = new AntidoteEater(player);
		notifier.dontNotify(tl);
		notifier.notifyInTurns(item.getAmount(), tl);
		item.removeOne();
		
		return true;
		
	}

}
