package org.stellar.anchor.reference.model;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.stellar.anchor.api.sep.sep38.PriceDetail;
import org.stellar.anchor.util.GsonUtils;
import shadow.com.google.common.reflect.TypeToken;

@Converter
public class PriceDetailListConverter implements AttributeConverter<List<PriceDetail>, String> {
  private static final Gson gson = GsonUtils.getInstance();

  @Override
  public String convertToDatabaseColumn(List<PriceDetail> priceDetails) {
    return gson.toJson(priceDetails);
  }

  @Override
  public List<PriceDetail> convertToEntityAttribute(String customerInfoJSON) {
    Type type = new TypeToken<List<PriceDetail>>() {}.getType();
    return gson.fromJson(customerInfoJSON, type);
  }
}
