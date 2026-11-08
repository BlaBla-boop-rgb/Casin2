package net.ccasino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Baut die Slot-Kacheln. Mit Resourcepack bekommt jedes Item ein eigenes
 * item_model aus dem CasinoPack ("ccasino:<name>"), ohne Pack einen
 * passenden Vanilla-Ersatz.
 */
public final class Tiles {
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
	private static final Map<String, ItemStack> DECO_CACHE = new HashMap<>();
	private static final Map<String, Character> GUI_CHARS = Map.of(
			"lobby", '', "slots", '', "roulette", '',
			"picker", '', "blackjack", '', "crash", '');

	private Tiles() {}

	public static Component text(String legacy) {
		return LEGACY.deserialize(legacy == null ? "" : legacy).decoration(TextDecoration.ITALIC, false);
	}

	/** Reine Deko-Kachel ohne Tooltip. */
	public static ItemStack deco(String model, boolean fancy) {
		String key = model + "|" + fancy;
		ItemStack cached = DECO_CACHE.get(key);
		if (cached != null) return cached;
		ItemStack it = new ItemStack(fallback(model));
		ItemMeta m = it.getItemMeta();
		if (fancy) m.setItemModel(new NamespacedKey("ccasino", model));
		m.setHideTooltip(true);
		it.setItemMeta(m);
		DECO_CACHE.put(key, it);
		return it;
	}

	/** Kachel mit Name und Lore (legacy §-Codes). */
	public static ItemStack item(String model, boolean fancy, String name, String... lore) {
		return item(model, fancy, false, name, lore);
	}

	public static ItemStack item(String model, boolean fancy, boolean glint, String name, String... lore) {
		ItemStack it = new ItemStack(fallback(model));
		ItemMeta m = it.getItemMeta();
		if (fancy) m.setItemModel(new NamespacedKey("ccasino", model));
		m.displayName(text(name));
		if (lore.length > 0) {
			List<Component> l = new ArrayList<>(lore.length);
			for (String s : lore) l.add(text(s));
			m.lore(l);
		}
		if (glint) m.setEnchantmentGlintOverride(true);
		it.setItemMeta(m);
		return it;
	}

	/** Inventar-Titel: mit Pack die Hintergrundgrafik, sonst normaler Text. */
	public static Component title(String gui, boolean fancy, String plain) {
		if (!fancy) return text(plain);
		Character bg = GUI_CHARS.get(gui);
		return Component.text(neg(8) + bg)
				.font(Key.key("ccasino", "gui"))
				.color(NamedTextColor.WHITE);
	}

	/** Negativer Abstand aus 2er-Potenzen (U+F001 = -1, U+F002 = -2, U+F003 = -4, …). */
	private static String neg(int px) {
		StringBuilder b = new StringBuilder();
		for (int k = 8; k >= 0; k--) {
			if ((px & (1 << k)) != 0) b.append((char) (0xF001 + k));
		}
		return b.toString();
	}

	// ------------------------------------------------------------------ Vanilla-Ersatz ohne Pack

	public static Material fallback(String model) {
		if (model.equals("felt") || model.startsWith("card_empty") || model.equals("pointer") || model.equals("pointer_up")
				|| model.startsWith("lbl_")) return model.startsWith("pointer") ? Material.ARROW : Material.GREEN_STAINED_GLASS_PANE;
		if (model.equals("panel") || model.startsWith("hist_none")) return Material.BLACK_STAINED_GLASS_PANE;
		if (model.equals("wood")) return Material.BROWN_STAINED_GLASS_PANE;
		if (model.startsWith("bulb_off")) return Material.ORANGE_STAINED_GLASS_PANE;
		if (model.startsWith("bulb")) return Material.YELLOW_STAINED_GLASS_PANE;
		if (model.startsWith("chip")) {
			char i = model.charAt(model.indexOf('_') + 1);
			return switch (i) {
				case '0' -> Material.BLUE_CONCRETE;
				case '1' -> Material.RED_CONCRETE;
				case '2' -> Material.LIME_CONCRETE;
				case '3' -> Material.PURPLE_CONCRETE;
				default -> Material.BLACK_CONCRETE;
			};
		}
		if (model.startsWith("led_g")) return Material.LIME_STAINED_GLASS_PANE;
		if (model.startsWith("led_r")) return Material.RED_STAINED_GLASS_PANE;
		if (model.startsWith("led")) return Material.GRAY_STAINED_GLASS_PANE;
		if (model.startsWith("ban_")) return Material.YELLOW_STAINED_GLASS_PANE;
		if (model.startsWith("btn_back")) return Material.ARROW;
		if (model.startsWith("btn_close")) return Material.BARRIER;
		if (model.startsWith("btn_info") || model.equals("paytable")) return Material.BOOK;
		if (model.endsWith("_off") || model.startsWith("wb_wait")) return Material.GRAY_DYE;
		if (model.startsWith("btn_spin") || model.startsWith("btn_stand")) return Material.RED_DYE;
		if (model.startsWith("btn_hit") || model.startsWith("wb_cash")) return Material.LIME_DYE;
		if (model.startsWith("btn_double")) return Material.PURPLE_DYE;
		if (model.startsWith("btn_deal") || model.startsWith("btn_repeat")) return Material.LIGHT_BLUE_DYE;
		if (model.startsWith("btn_clear")) return Material.LAVA_BUCKET;
		if (model.startsWith("wb_launch")) return Material.ORANGE_DYE;
		if (model.equals("coin")) return Material.GOLD_NUGGET;
		if (model.equals("game_slots")) return Material.NETHER_STAR;
		if (model.equals("game_roulette")) return Material.CLOCK;
		if (model.equals("game_blackjack")) return Material.PAPER;
		if (model.equals("game_crash")) return Material.FIREWORK_ROCKET;
		if (model.startsWith("sym_")) {
			return switch (model.charAt(4)) {
				case '0' -> Material.SWEET_BERRIES;
				case '1' -> Material.REDSTONE;
				case '2' -> Material.IRON_INGOT;
				case '3' -> Material.GOLD_INGOT;
				case '4' -> Material.EMERALD;
				case '5' -> Material.DIAMOND;
				default -> Material.NETHER_STAR;
			};
		}
		if (model.startsWith("cab_arrow")) return Material.SPECTRAL_ARROW;
		if (model.startsWith("cab")) return Material.RED_STAINED_GLASS_PANE;
		if (model.startsWith("lever_top_empty")) return Material.RED_STAINED_GLASS_PANE;
		if (model.startsWith("lever")) return Material.LEVER;
		if (model.startsWith("rn_")) {
			int n = Integer.parseInt(model.substring(3).replace("_hl", ""));
			if (n == 0) return Material.LIME_CONCRETE;
			return isRed(n) ? Material.RED_CONCRETE : Material.BLACK_CONCRETE;
		}
		if (model.equals("bet_red")) return Material.RED_WOOL;
		if (model.equals("bet_black")) return Material.BLACK_WOOL;
		if (model.equals("bet_zero")) return Material.LIME_WOOL;
		if (model.equals("bet_pick")) return Material.NAME_TAG;
		if (model.startsWith("bet_")) return Material.WHITE_WOOL;
		if (model.equals("card_back")) return Material.RED_STAINED_GLASS_PANE;
		if (model.startsWith("card_")) return Material.PAPER;
		if (model.startsWith("rocket")) return Material.FIREWORK_ROCKET;
		if (model.startsWith("flame")) return Material.BLAZE_POWDER;
		if (model.equals("boom")) return Material.FIRE_CHARGE;
		if (model.startsWith("sky")) return Material.BLUE_STAINED_GLASS_PANE;
		if (model.equals("hist_red")) return Material.RED_STAINED_GLASS_PANE;
		if (model.equals("hist_green")) return Material.LIME_STAINED_GLASS_PANE;
		if (model.equals("hist_gold")) return Material.YELLOW_STAINED_GLASS_PANE;
		return Material.GRAY_STAINED_GLASS_PANE;
	}

	private static final boolean[] RED = new boolean[37];
	static {
		for (int n : new int[] {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36}) RED[n] = true;
	}

	public static boolean isRed(int n) { return n > 0 && n < 37 && RED[n]; }
}
