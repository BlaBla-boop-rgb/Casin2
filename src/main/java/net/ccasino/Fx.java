package net.ccasino;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Sounds und Partikel – nur für den spielenden Spieler. */
public final class Fx {
	private Fx() {}

	public static void sound(Player p, Sound s, float pitch, float vol) {
		p.playSound(p.getLocation(), s, vol, pitch);
	}

	public static void click(Player p) { sound(p, Sound.UI_BUTTON_CLICK, 1f, 0.5f); }
	public static void chip(Player p) { sound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.8f, 0.6f); }
	public static void tick(Player p, float pitch) { sound(p, Sound.BLOCK_NOTE_BLOCK_HAT, pitch, 0.35f); }
	public static void thunk(Player p) { sound(p, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.8f, 0.7f); }
	public static void card(Player p) { sound(p, Sound.ITEM_BOOK_PAGE_TURN, 1.3f, 0.9f); }
	public static void lose(Player p) { sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f); }
	public static void bell(Player p, float pitch) { sound(p, Sound.BLOCK_NOTE_BLOCK_BELL, pitch, 0.6f); }
	public static void error(Player p) { sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f); }

	public static void win(Player p, boolean big) {
		if (big) {
			sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
			burst(p, Particle.TOTEM_OF_UNDYING, 80);
		} else {
			sound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 0.7f);
			burst(p, Particle.HAPPY_VILLAGER, 20);
		}
		sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 0.6f);
	}

	public static void explode(Player p) {
		sound(p, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.4f);
		Location l = p.getLocation().add(p.getLocation().getDirection().multiply(2)).add(0, 1.4, 0);
		p.spawnParticle(Particle.EXPLOSION, l, 1);
	}

	public static void burst(Player p, Particle particle, int n) {
		Location l = p.getLocation().add(0, 1.2, 0);
		p.spawnParticle(particle, l, n, 0.6, 0.6, 0.6, 0.25);
	}
}
