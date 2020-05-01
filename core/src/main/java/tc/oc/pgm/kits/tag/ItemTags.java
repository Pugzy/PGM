package tc.oc.pgm.kits.tag;

import tc.oc.pgm.util.item.tag.BooleanItemTag;
import tc.oc.pgm.util.item.tag.StringItemTag;

public class ItemTags {

  public static final BooleanItemTag PREVENT_SHARING = new BooleanItemTag("prevent-sharing", false);
  public static final StringItemTag PROJECTILE = new StringItemTag("projectile", null);
  public static final StringItemTag ORIGINAL_NAME = new StringItemTag("original-name", null);
  public static final BooleanItemTag TEAM_COLOR = new BooleanItemTag("team-color", false);

  private ItemTags() {}
}
