package org.stellar.anchor.asset;

import java.io.IOException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.util.FileUtil;

public class ResourceJsonAssetService extends JsonAssetService {
  public ResourceJsonAssetService(String assetPath) throws IOException, SepNotFoundException {
    super(FileUtil.getResourceFileAsString(assetPath));
  }
}
