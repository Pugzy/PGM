package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.block.BlockState;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.snapshot.SnapshotMatchModule;

/** Matches blocks that are from the original level */
public class OriginalFilter extends TypedFilter<BlockQuery> {

  private final Filter child;

  public OriginalFilter(Filter child) {
    checkNotNull(child);
    this.child = child;
  }

  @Override
  public Class<? extends BlockQuery> getQueryType() {
    return BlockQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(BlockQuery query) {
    SnapshotMatchModule smm = query.getMatch().getModule(SnapshotMatchModule.class);
    if (smm == null) return QueryResponse.ABSTAIN;

    BlockState originalBlock = smm.getOriginalBlock(query.getBlock().getLocation().toVector());
    query = new tc.oc.pgm.filters.query.BlockQuery(query.getEvent(), originalBlock);

    return child.query(query);
  }

  @Override
  public String toString() {
    return "OriginalFilter{}";
  }
}
