package com.eric6166.order.service;

import com.eric6166.framework.kafka.Event;
import com.eric6166.order.client.InventoryClient;
import com.eric6166.order.dto.InventoryResponse;
import com.eric6166.order.dto.OrderLineItemsDto;
import com.eric6166.order.dto.OrderRequest;
import com.eric6166.order.enums.TypeReq;
import com.eric6166.order.event.OrderPlacedEvent;
import com.eric6166.order.kafka.KafkaProducerProperties;
import com.eric6166.order.model.Order;
import com.eric6166.order.model.OrderLineItem;
import com.eric6166.order.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderService {

    final OrderRepository orderRepository;

    final WebClient.Builder webClientBuilder;
//
//    final Tracer tracer;
//
    final KafkaTemplate<String, Object> kafkaTemplate;

    final InventoryClient inventoryClient;

    final KafkaProducerProperties kafkaProducerProperties;

    public String placeOrder(OrderRequest orderRequest, TypeReq typeReq) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItem> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemList(orderLineItems);
        List<String> skuCodes = order.getOrderLineItemList().stream()
                .map(OrderLineItem::getSkuCode)
                .toList();
        List<InventoryResponse> inventoryResponseList = new ArrayList<>();
        switch (typeReq) {
            case WEB_CLIENT -> inventoryResponseList = Arrays.stream(webClientBuilder.build().get()
                            .uri("http://inventory-service/api/inventory",
                                    uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                            .retrieve()
                            .bodyToMono(InventoryResponse[].class)
                            .block())
                    .toList();
            case FEIGN_CLIENT -> inventoryClient.searchInventory(skuCodes);
            default -> {
            }
        }
        boolean allProductsInStock = false;
        if (ObjectUtils.isNotEmpty(inventoryResponseList)) {
            allProductsInStock = inventoryResponseList.stream()
                    .allMatch(InventoryResponse::isInStock);
        }
        if (Boolean.TRUE.equals(allProductsInStock)) {
            orderRepository.saveAndFlush(order);
            kafkaTemplate.send(kafkaProducerProperties.getNotificationTopicName(), Event.builder()
                    .payload(OrderPlacedEvent.builder()
                            .orderNumber(order.getOrderNumber())
                            .build())
                    .build());
            kafkaTemplate.send(kafkaProducerProperties.getInternalTopicName(), Event.builder()
                    .payload(OrderPlacedEvent.builder()
                            .orderNumber(order.getOrderNumber())
                            .build())
                    .build());
            log.info("inventoryResponseList : {}", inventoryResponseList);
            return "Order Placed";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }



//        Span span = tracer.nextSpan().name("inventory-service-lookup");
//        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
//            InventoryResponse[] inventoryResponseArray = webClient.build().get()
//                    .uri("http://inventory-service/api/inventory",
//                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
//                    .retrieve()
//                    .bodyToMono(InventoryResponse[].class)
//                    .block();

//            boolean allProductsInStock = false;
//            if (ObjectUtils.isNotEmpty(inventoryResponseArray)) {
//                allProductsInStock = Arrays.stream(inventoryResponseArray)
//                        .allMatch(InventoryResponse::isInStock);
//            }
//            if (Boolean.TRUE.equals(allProductsInStock)) {
//                orderRepository.saveAndFlush(order);
//                kafkaTemplate.send("notificationTopic", AppEvent.builder()
//                        .payload(OrderPlacedEvent.builder()
//                                .orderNumber(order.getOrderNumber())
//                                .build())
//                        .build());
//                return "Order Placed";
//            } else {
//                throw new IllegalArgumentException("Product is not in stock, please try again later");
//            }
//        } finally {
//            span.end();
//        }
    }


    private OrderLineItem mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItem orderLineItem = new OrderLineItem();
        orderLineItem.setPrice(orderLineItemsDto.getPrice());
        orderLineItem.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItem.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItem;
    }

}

