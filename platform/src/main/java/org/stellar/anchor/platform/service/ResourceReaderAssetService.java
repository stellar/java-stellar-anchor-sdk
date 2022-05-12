package org.stellar.anchor.platform.service;

import org.stellar.anchor.asset.JsonAssetService;
import org.stellar.anchor.util.ResourceReader;

public class ResourceReaderAssetService extends JsonAssetService {
  public ResourceReaderAssetService(String assets) {
    this(assets, new SpringResourceReader());
  }

  public ResourceReaderAssetService(String assets, ResourceReader resourceReader) {
    super(resourceReader.readResourceAsString(assets));
  }
}
