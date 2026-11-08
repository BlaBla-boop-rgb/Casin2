package net.ccasino.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.ccasino.Fx;
import net.ccasino.Session;
import org.bukkit.inventory.Inventory;

/** Blackjack: Dealer oben (Reihe 1), Spieler unten (Reihe 3), Karten werden nacheinander ausgeteilt. */
public class BlackjackScreen extends Screen {
	private static final String[] RANKS = {"a", "2", "3", "4", "5", "6", "7", "8", "9", "10", "j", "q", "k"};
	private static final String[] RANK_NAMES = {"Ass", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Bube", "Dame", "König"};
	private static final String[] SUITS = {"s", "h", "d", "c"};
	private static final String[] SUIT_NAMES = {"§8♠ Pik", "§c♥ Herz", "§c♦ Karo", "§8♣ Kreuz"};
	private static final int DEAL_GAP = 7; // Ticks zwischen zwei Karten

	private enum State { IDLE, DEALING, PLAYER, DEALER, DONE }

	private static final class Card {
		final int rank, suit;
		long showAt;
		long revealAt = -1; // -1 = verdeckt

		Card(int rank, int suit) {
			this.rank = rank;
			this.suit = suit;
		}

		int value() { return rank == 0 ? 11 : Math.min(10, rank + 1); }
	}

	private final List<Integer> shoe = new ArrayList<>();
	private final List<Card> dealer = new ArrayList<>();
	private final List<Card> player = new ArrayList<>();
	private State state = State.IDLE;
	private double stake, payout;
	private long lastDeal, nextAction = -1;
	private Runnable action;
	private String banner;
	private String bannerTitle;

	public BlackjackScreen(Session s) {
		super(s);
		reshuffle();
	}

	@Override protected String gui() { return "blackjack"; }
	@Override protected String plainTitle() { return "§6Craft Casino §8» §9Blackjack"; }
	@Override public boolean busy() { return state != State.IDLE && state != State.DONE; }

	@Override
	protected String[] info() {
		return new String[] {"§7Komm näher an 21 als der Dealer,", "§7ohne darüber zu gehen.", "",
				"§7Blackjack (Ass + 10er) zahlt §63:2", "§7Sieg zahlt §e2x§7, Gleichstand = Einsatz zurück",
				"§7Dealer zieht bis 16 und steht ab 17", "§7Doppeln: Einsatz ×2, genau eine Karte"};
	}

	// ------------------------------------------------------------------ Logik

	private void reshuffle() {
		shoe.clear();
		for (int d = 0; d < 6; d++) for (int c = 0; c < 52; c++) shoe.add(c);
		Collections.shuffle(shoe, rnd);
	}

	private Card draw() {
		if (shoe.isEmpty()) reshuffle();
		int c = shoe.remove(shoe.size() - 1);
		return new Card(c % 13, c / 13);
	}

	private static int total(List<Card> hand, long now, boolean visibleOnly) {
		int sum = 0, aces = 0;
		for (Card c : hand) {
			if (visibleOnly && (c.revealAt < 0 || now < c.revealAt)) continue;
			sum += c.value();
			if (c.rank == 0) aces++;
		}
		while (sum > 21 && aces > 0) {
			sum -= 10;
			aces--;
		}
		return sum;
	}

	private int total(List<Card> hand) { return total(hand, 0, false); }

	private boolean blackjack(List<Card> hand) { return hand.size() == 2 && total(hand) == 21; }

	private void give(List<Card> hand, boolean faceUp) {
		Card c = draw();
		long at = Math.max(s.tick(), lastDeal + DEAL_GAP);
		lastDeal = at;
		c.showAt = at;
		c.revealAt = faceUp ? at + 2 : -1;
		hand.add(c);
	}

	private void later(long at, Runnable r) {
		nextAction = at;
		action = r;
	}

	private void deal() {
		if (busy()) return;
		double b = s.bet();
		if (!pl.take(p, b)) {
			Fx.error(p);
			return;
		}
		if (shoe.size() < 60) reshuffle();
		stake = b;
		dealer.clear();
		player.clear();
		banner = null;
		state = State.DEALING;
		lastDeal = s.tick() - DEAL_GAP + 2;
		give(player, true);
		give(dealer, true);
		give(player, true);
		give(dealer, false);
		later(lastDeal + 6, this::afterDeal);
	}

	private void afterDeal() {
		if (blackjack(player) || blackjack(dealer)) {
			reveal();
			state = State.DEALER;
			later(s.tick() + 14, this::settle);
		} else {
			state = State.PLAYER;
		}
	}

	private void hit() {
		if (state != State.PLAYER) return;
		give(player, true);
		state = State.DEALING;
		later(lastDeal + 4, () -> {
			int t = total(player);
			if (t > 21) {
				reveal();
				state = State.DEALER;
				later(s.tick() + 12, this::settle);
			} else if (t == 21) {
				stand();
			} else {
				state = State.PLAYER;
			}
		});
	}

	private void doubleDown() {
		if (state != State.PLAYER || player.size() != 2) return;
		if (!pl.take(p, stake)) {
			Fx.error(p);
			return;
		}
		stake *= 2;
		give(player, true);
		state = State.DEALING;
		later(lastDeal + 4, () -> {
			if (total(player) > 21) {
				reveal();
				state = State.DEALER;
				later(s.tick() + 12, this::settle);
			} else {
				stand();
			}
		});
	}

	private void stand() {
		state = State.DEALER;
		reveal();
		later(s.tick() + 12, this::dealerStep);
	}

	private void dealerStep() {
		if (total(dealer) < 17) {
			give(dealer, true);
			later(lastDeal + 10, this::dealerStep);
		} else {
			settle();
		}
	}

	private void reveal() {
		for (Card c : dealer) {
			if (c.revealAt < 0) {
				c.revealAt = Math.max(s.tick(), c.showAt + 2);
				Fx.card(p);
			}
		}
	}

	private void settle() {
		int pt = total(player), dt = total(dealer);
		boolean pbj = blackjack(player), dbj = blackjack(dealer);
		if (pt > 21) { payout = 0; banner = "bust"; bannerTitle = "§cÜberkauft!"; }
		else if (pbj && !dbj) { payout = stake * 2.5; banner = "bj"; bannerTitle = "§6§lBLACKJACK!"; }
		else if (dbj && !pbj) { payout = 0; banner = "lose"; bannerTitle = "§cDealer hat Blackjack"; }
		else if (dt > 21) { payout = stake * 2; banner = "dbust"; bannerTitle = "§aDealer überkauft – gewonnen!"; }
		else if (pt > dt) { payout = stake * 2; banner = "win"; bannerTitle = "§aGewonnen!"; }
		else if (pt == dt) { payout = stake; banner = "push"; bannerTitle = "§fUnentschieden"; }
		else { payout = 0; banner = "lose"; bannerTitle = "§cVerloren"; }
		state = State.DONE;
		action = null;
		payout = Math.floor(payout);
		if (payout > 0) pl.give(p, payout);
		if (payout > stake) {
			s.party(pbj ? 80 : 40);
			Fx.win(p, pbj);
		} else if (payout == stake) {
			Fx.bell(p, 1f);
		} else {
			Fx.lose(p);
		}
	}

	@Override
	public void forceFinish() {
		if (!busy()) return;
		action = null;
		if (total(player) <= 21 && !blackjack(player) && !blackjack(dealer)) {
			while (total(dealer) < 17) dealer.add(draw());
		}
		settle();
	}

	@Override
	public void tick() {
		long t = s.tick();
		for (List<Card> hand : List.of(dealer, player)) {
			for (Card c : hand) {
				if (c.showAt == t) Fx.card(p);
			}
		}
		if (action != null && t >= nextAction) {
			Runnable r = action;
			action = null;
			r.run();
		}
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	protected void draw(Inventory inv) {
		long t = s.tick();
		drawHand(inv, 1, dealer, t, "§7Dealer");
		drawHand(inv, 3, player, t, "§7Deine Hand");

		// Summen als LED
		if (!dealer.isEmpty()) {
			int v = total(dealer, t, true);
			led(inv, slot(1, 0), 2, String.valueOf(v), v > 21 ? 'r' : 'a', "§7Dealer: §e" + v);
		} else {
			inv.setItem(slot(1, 0), item("lbl_dealer", "§7Dealer"));
			inv.setItem(slot(1, 1), deco("felt"));
		}
		if (!player.isEmpty()) {
			int v = total(player, t, true);
			char col = v > 21 ? 'r' : (v == 21 ? 'g' : 'a');
			led(inv, slot(3, 0), 2, String.valueOf(v), col, "§7Deine Summe: §e" + v);
		} else {
			inv.setItem(slot(3, 0), item("lbl_du", "§7Du"));
			inv.setItem(slot(3, 1), deco("felt"));
		}

		// Mitte: Ergebnis-Banner oder Hinweis
		if (state == State.DONE && banner != null) {
			banner(inv, slot(2, 2), banner, bannerTitle + (payout > 0 ? " §8(§e+" + pl.money(payout) + "§8)" : ""));
		} else {
			inv.setItem(slot(2, 4), item("lbl_32", "§6Blackjack zahlt 3:2", "§7Dealer steht ab 17"));
		}

		// Einsatz-Chip
		if (state != State.IDLE) {
			inv.setItem(slot(4, 0), item("chipf_" + s.betIndex(), "§6Einsatz: §e" + pl.money(stake)));
		}

		// Knöpfe
		if (state == State.PLAYER) {
			inv.setItem(slot(4, 3), item("btn_hit", "§a§lKARTE", "§7Noch eine Karte ziehen"));
			inv.setItem(slot(4, 5), item("btn_stand", "§c§lHALTEN", "§7Dealer ist dran"));
			boolean canDouble = player.size() == 2 && pl.balance(p) >= stake;
			inv.setItem(slot(4, 7), canDouble
					? item("btn_double", "§5§lDOPPELN", "§7Einsatz verdoppeln (+" + pl.money(stake) + ")", "§7genau eine Karte")
					: item("btn_double_off", "§8Doppeln nicht möglich"));
		} else if (!busy()) {
			inv.setItem(slot(4, 5), item("btn_deal", "§9§lAUSTEILEN", "§7Einsatz: §e" + pl.money(s.bet())));
		}
	}

	private void drawHand(Inventory inv, int row, List<Card> hand, long t, String owner) {
		for (int i = 0; i < 7; i++) {
			int sl = slot(row, 2 + i);
			if (i < hand.size() && t >= hand.get(i).showAt) {
				Card c = hand.get(i);
				boolean up = c.revealAt >= 0 && t >= c.revealAt;
				inv.setItem(sl, up
						? item("card_" + RANKS[c.rank] + "_" + SUITS[c.suit], SUIT_NAMES[c.suit] + " §f" + RANK_NAMES[c.rank], owner)
						: item("card_back", "§7Verdeckte Karte", owner));
			} else if (i < 2 || i < hand.size()) {
				inv.setItem(sl, deco("card_empty"));
			}
		}
	}

	@Override
	protected void onClick(int slot, boolean right) {
		int row = slot / 9, col = slot % 9;
		if (row != 4) return;
		if (state == State.PLAYER) {
			if (col == 3) { Fx.click(p); hit(); }
			else if (col == 5) { Fx.click(p); stand(); }
			else if (col == 7) { Fx.click(p); doubleDown(); }
		} else if (!busy() && col == 5) {
			deal();
		}
	}
}
