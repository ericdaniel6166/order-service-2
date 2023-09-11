package com.eric6166.order.client;

import com.eric6166.order.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory")
    List<InventoryResponse> searchInventory(@RequestParam List<String> skuCode);
}
