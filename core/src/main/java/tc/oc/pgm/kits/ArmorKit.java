package tc.oc.pgm.kits;

import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class ArmorKit extends AbstractKit {
  public static class ArmorItem {
    public final ItemStack stack;
    public final boolean locked;

    public ArmorItem(ItemStack stack, boolean locked) {
      this.stack = stack;
      this.locked = locked;
    }
  }

  private final Map<ArmorType, ArmorItem> armor;

  public ArmorKit(Map<ArmorType, ArmorItem> armor) {
    this.armor = armor;
  }

  public Map<ArmorType, ArmorItem> getArmor() {
    return armor;
  }

  /**
   * If force is true, existing armor is always replaced. If false, it is never replaced. TODO:
   * repair armor, upgrade world
   */
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    ItemStack[] wearing = player.getBukkit().getInventory().getArmorContents();
    for (Map.Entry<ArmorType, ArmorItem> entry : this.armor.entrySet()) {
      int slot = entry.getKey().ordinal();
      if (force || wearing[slot] == null || wearing[slot].getType() == Material.AIR) {

        ItemStack item = entry.getValue().stack.clone();

        if (ItemTags.TEAM_COLOR.has(item)) {
          ItemMeta itemMeta = item.getItemMeta();

          if (itemMeta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leather = (LeatherArmorMeta) itemMeta;
            Color primary =
                BukkitUtils.chatColorToDyeColor(player.getParty().getColor()).getColor();
            leather.setColor(primary);
          }

          item.setItemMeta(itemMeta);
        }

        wearing[slot] = item;

        KitMatchModule kitMatchModule = player.getMatch().getModule(KitMatchModule.class);
        if (kitMatchModule != null) {
          kitMatchModule.lockArmorSlot(player, entry.getKey(), entry.getValue().locked);
        }
      }
    }
    player.getBukkit().getInventory().setArmorContents(wearing);
  }
}
