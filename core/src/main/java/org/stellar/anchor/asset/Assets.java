package org.stellar.anchor.asset;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

@Data
public class Assets {
  List<AssetInfo> assets;
}
