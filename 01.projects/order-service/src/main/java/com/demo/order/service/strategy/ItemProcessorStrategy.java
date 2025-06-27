package com.demo.order.service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import com.demo.order.service.model.EnumProductStatus;
import com.demo.order.service.model.Order;
import com.demo.order.service.model.OrderDispatcherWrapper;
import com.demo.order.service.model.Product;
import com.demo.order.service.model.ProductEntity;
import com.demo.order.service.model.ProductProcessable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ItemProcessorStrategy implements AggregationStrategy {

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

    if (oldExchange == null) {
      log.info("Create new instance of wrapper");
      var orderDispatcherWrapper = new OrderDispatcherWrapper();
      var product = newExchange.getIn().getBody(Product.class);
      var order = newExchange.getProperty("orderDispatcher", Order.class);
      List<ProductEntity> productEntities =
          newExchange.getProperty("orderProductEntities", List.class);
      orderDispatcherWrapper.setClient(order.getClient());
      orderDispatcherWrapper.setDate(
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      orderDispatcherWrapper.setTotal(new BigDecimal(0.0));
      orderDispatcherWrapper.setTax(new BigDecimal(0.0));
      orderDispatcherWrapper.setTotalWithoutTax(new BigDecimal(0.0));
      processItem(product, productEntities, orderDispatcherWrapper);
      newExchange.getIn().setBody(orderDispatcherWrapper);
      return newExchange;
    }

    var product = newExchange.getIn().getBody(Product.class);
    List<ProductEntity> productEntities =
        newExchange.getProperty("orderProductEntities", List.class);
    var orderDispatcherWrapper = oldExchange.getIn().getBody(OrderDispatcherWrapper.class);
    processItem(product, productEntities, orderDispatcherWrapper);
    newExchange.getIn().setBody(orderDispatcherWrapper);
    return newExchange;
  }

  private void processItem(
      Product product,
      List<ProductEntity> productEntities,
      OrderDispatcherWrapper orderDispatcherWrapper) {

    Optional<ProductEntity> productEntity =
        productEntities.stream()
            .filter(item -> item.getCode().equals(product.getCode()) && item.getIsActive())
            .findFirst();
    var processable = new ProductProcessable();
    processable.setProduct(product);

    if (productEntity.isPresent()) {
      log.info("The product is active, calculate price...");
      // Producto disponible
      int quantity =
          productEntity.get().getStockQuantity() < product.getQuantity()
              ? productEntity.get().getStockQuantity()
              : product.getQuantity();
      // Recalcular el total si cambia el precio o no hay suficiente cantidad de producto
      if (!product.getPrice().equals(productEntity.get().getPrice())
          || productEntity.get().getStockQuantity() < product.getQuantity()) {
        log.info("The product has changed price or not enough in stock.");
        // Recalcular precio por item y tax
        priceAndTaxCalculator(
            orderDispatcherWrapper,
            productEntity.get().getPrice(),
            productEntity.get().getTax(),
            quantity);

        if (!product.getPrice().equals(productEntity.get().getPrice())) {
          log.info("The product has change in price...");
          processable.setReason(
              String.format("El producto %s cambió de precio.", product.getName()));
          processable.setStatus(EnumProductStatus.PRICE_CHANGED);
        }
        if (productEntity.get().getStockQuantity() > 0
            && productEntity.get().getStockQuantity() < product.getQuantity()) {
          log.info("Not enough product in stock...");
          processable.setReason(
              String.format(
                  "El producto %s no hay suficientes en stock, su pedido se realizará al final del dia.",
                  product.getName()));
          processable.setStatus(EnumProductStatus.NOT_ENOUGH_IN_STOCK);
        }
        if (productEntity.get().getStockQuantity() <= 0) {
          log.info("There is not product in stock...");
          processable.setReason(
              String.format(
                  "El producto %s no hay en stock, su pedido se realizará al final del dia.",
                  product.getName()));
          processable.setStatus(EnumProductStatus.OUT_OF_STOCK);
        }
        // Actualizar el precio y cantidad
        product.setPrice(productEntity.get().getPrice());
        product.setQuantity(quantity);
        if (processable.getStatus().equals(EnumProductStatus.NOT_ENOUGH_IN_STOCK)
            || processable.getStatus().equals(EnumProductStatus.OUT_OF_STOCK)) {
          orderDispatcherWrapper.addBackOrderProducts(processable);
        } else {
          orderDispatcherWrapper.addProcessableProduct(processable);
        }
      } else {
        log.info("Product available and is being processed...");
        priceAndTaxCalculator(
            orderDispatcherWrapper, product.getPrice(), productEntity.get().getTax(), quantity);

        processable.setReason(
            String.format("El producto %s se esta procesando.", product.getName()));
        processable.setStatus(EnumProductStatus.AVAILABLE);
        orderDispatcherWrapper.addProcessableProduct(processable);
      }
    } else {
      log.info("Product is not active, not processable.");
      // Producto no disponible
      processable.setReason(
          String.format("El producto %s no esta en existencia.", product.getName()));
      processable.setStatus(EnumProductStatus.UNAVAILABLE);
      orderDispatcherWrapper.addNotProcessableProduct(processable);
    }
  }

  /**
   * Calculate the price and tax of current product, update the dispatcher total price and tax as
   * well.
   *
   * @param orderDispatcherWrapper dispatcher order object.
   * @param price the product price.
   * @param tax the product tax applicable.
   * @param quantity product quantity.
   */
  private void priceAndTaxCalculator(
      OrderDispatcherWrapper orderDispatcherWrapper,
      BigDecimal price,
      BigDecimal tax,
      int quantity) {
    // Actualizar el total y el tax
    var priceCalculated = priceCalculator(price, quantity);
    var taxCalculated = taxCalculator(tax, quantity, price);

    orderDispatcherWrapper.setTotal(
        orderDispatcherWrapper.getTotal().add(priceCalculated).add(taxCalculated));
    orderDispatcherWrapper.setTax(orderDispatcherWrapper.getTax().add(taxCalculated));
    orderDispatcherWrapper.setTotalWithoutTax(
        orderDispatcherWrapper.getTotalWithoutTax().add(priceCalculated));
  }

  /**
   * Calculate the tax applicable to a given quantity.
   *
   * @param taxPercentage the tax applicable.
   * @param quantity the product quantity.
   * @return tax calculated.
   */
  private BigDecimal taxCalculator(BigDecimal taxPercentage, int quantity, BigDecimal price) {
    return price
        .multiply(BigDecimal.valueOf(quantity))
        .multiply(taxPercentage.divide(new BigDecimal(100)))
        .setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * calculate the price of given quantity product.
   *
   * @param price the product price.
   * @param quantity the product quantity.
   * @return price calculated.
   */
  private BigDecimal priceCalculator(BigDecimal price, int quantity) {
    return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
  }
}
