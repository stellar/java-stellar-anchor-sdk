package org.stellar.anchor.platform.payment.observer.circle.model.response;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CircleListResponse<T> extends CircleDetailResponse<List<T>> {}
