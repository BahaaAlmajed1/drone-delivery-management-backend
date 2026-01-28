package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.security.AuthContext;
import com.example.dronedelivery.service.DeliveryService;
import com.example.dronedelivery.service.Mapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/enduser/orders")
@PreAuthorize("hasRole('ENDUSER')")
public class EndUserOrdersController {

    private final DeliveryService deliveryService;

    public EndUserOrdersController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public OrderDtos.OrderResponse submit(@Valid @RequestBody OrderDtos.SubmitOrderRequest req) {
        UUID endUserId = AuthContext.actorIdOrNull();
        var order = deliveryService.submitOrder(
                endUserId,
                req.origin().lat(), req.origin().lng(),
                req.destination().lat(), req.destination().lng()
        );
        return Mapper.toDto(order, deliveryService.computeProgress(order));
    }

    @PostMapping("/{orderId}/withdraw")
    public OrderDtos.OrderResponse withdraw(@PathVariable UUID orderId) {
        UUID endUserId = AuthContext.actorIdOrNull();
        var order = deliveryService.withdrawOrder(endUserId, orderId);
        return Mapper.toDto(order, deliveryService.computeProgress(order));
    }

    @GetMapping
    public List<OrderDtos.OrderResponse> listMine() {
        UUID endUserId = AuthContext.actorIdOrNull();
        return deliveryService.listOrdersForEndUser(endUserId)
                .stream()
                .map(o -> Mapper.toDto(o, deliveryService.computeProgress(o)))
                .toList();
    }

    @GetMapping("/{orderId}")
    public OrderDtos.OrderResponse getMine(@PathVariable UUID orderId) {
        UUID endUserId = AuthContext.actorIdOrNull();
        var order = deliveryService.getOrderForEndUser(endUserId, orderId);
        return Mapper.toDto(order, deliveryService.computeProgress(order));
    }
}
