package com.eric6166.order.controller;

import com.eric6166.framework.kafka.KafkaProperties;
import com.eric6166.order.dto.OrderRequest;
import com.eric6166.order.enums.TypeReq;
import com.eric6166.order.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderController {

    final OrderService orderService;

    @GetMapping
    public String sampleOrder() {
        log.info("Sample order");
        return "Sample Order";
    }

    @PostMapping("/web-client")
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrderWebClient(@RequestBody OrderRequest orderRequest) {
        log.info("Placing Order");
        return orderService.placeOrder(orderRequest, TypeReq.WEB_CLIENT);
    }

    @PostMapping("/feign-client")
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrderFeign(@RequestBody OrderRequest orderRequest) {
        log.info("Placing Order");
        return orderService.placeOrder(orderRequest, TypeReq.FEIGN_CLIENT);
    }

//    public CompletableFuture<String> placeOrder(@RequestBody OrderRequest orderRequest) {
//        log.info("Placing Order");
//        return CompletableFuture.supplyAsync(() -> orderService.placeOrder(orderRequest));
//    }
//
//

//    public CompletableFuture<String> fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
//        log.info("Cannot Place Order, Executing Fallback logic");
//        return CompletableFuture.supplyAsync(() -> "Something went wrong, please order after some time!");
//    }
}
