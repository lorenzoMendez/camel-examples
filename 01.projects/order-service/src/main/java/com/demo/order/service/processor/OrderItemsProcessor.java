package com.demo.order.service.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.demo.order.service.model.EnumProductStatus;
import com.demo.order.service.model.Order;
import com.demo.order.service.model.OrderDispatcherWrapper;
import com.demo.order.service.model.Product;
import com.demo.order.service.model.ProductEntity;
import com.demo.order.service.model.ProductProcessable;
import com.demo.order.service.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderItemsProcessor implements Processor {

  private final ProductRepository productRepository;

  public OrderItemsProcessor(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  /**
   * Process the incoming order and update the exchange message.
   *
   * @param exchange the exchange with order message.
   */
  @Override
  public void process(Exchange exchange) throws Exception {
    var order = exchange.getIn().getBody(Order.class);

    var orderDispatcherWrapper = new OrderDispatcherWrapper();
    orderDispatcherWrapper.setClient(order.getClient());
    orderDispatcherWrapper.setDate(
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    orderDispatcherWrapper.setTotal(new BigDecimal(0.0));
    orderDispatcherWrapper.setTax(new BigDecimal(0.0));
    orderDispatcherWrapper.setTotalWithoutTax(new BigDecimal(0.0));

    List<String> codes =
        order.getProducts().stream().map(Product::getCode).collect(Collectors.toList());

    log.info("=== Product codes: {}", codes);

    List<ProductEntity> products = productRepository.findByCodeIn(codes);
    log.debug("=== Items: {}", products);
    log.info("Items found: {}", products.size());

    processOrderDispatcher(orderDispatcherWrapper, order.getProducts(), products);

    // Regresamos el objeto procesado.
    exchange.getIn().setBody(orderDispatcherWrapper);
  }

  /**
   * Process the order according the product availability and categorized them if are processables
   * or not.
   *
   * @param orderDispatcherWrapper new object to hold the product stataus.
   * @param products the products coming from queue.
   * @param productEntities the product found in database.
   */
  private void processOrderDispatcher(
      OrderDispatcherWrapper orderDispatcherWrapper,
      List<Product> products,
      List<ProductEntity> productEntities) {

    for (var product : products) {

      log.info(
          "Product: {}, quantity: {}, price: {}",
          product.getName(),
          product.getQuantity(),
          product.getPrice());

      Optional<ProductEntity> productEntity =
          productEntities.stream()
              .filter(item -> item.getCode().equals(product.getCode()) && item.getIsActive())
              .findFirst();

      log.info("Product is exist: {}", productEntity.isPresent());

      var processable = new ProductProcessable();
      processable.setProduct(product);
      // Validar disponibilidad
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
