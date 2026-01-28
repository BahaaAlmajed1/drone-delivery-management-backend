package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.security.AuthContext;
import com.example.dronedelivery.service.EndUserService;
import com.example.dronedelivery.service.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/enduser/orders")
@PreAuthorize("hasRole('ENDUSER')")
public class EndUserOrdersController {

    private final EndUserService endUserService;

    public EndUserOrdersController(EndUserService endUserService) {
        this.endUserService = endUserService;
    }

    @PostMapping
    public OrderDtos.OrderResponse submit(@Valid @RequestBody OrderDtos.SubmitOrderRequest req) {
        UUID endUserId = AuthContext.actorIdOrNull();
        var order = endUserService.submitOrder(
                endUserId,
                req.origin().lat(), req.origin().lng(),
                req.destination().lat(), req.destination().lng()
        );
        return ResponseMapper.toDto(order, endUserService.computeProgress(order));
    }

    @PostMapping("/{orderId}/withdraw")
    public OrderDtos.OrderResponse withdraw(@PathVariable UUID orderId) {
        UUID endUserId = AuthContext.actorIdOrNull();
        var order = endUserService.withdrawOrder(endUserId, orderId);
        return ResponseMapper.toDto(order, endUserService.computeProgress(order));
    }

    @GetMapping
    public List<OrderDtos.OrderResponse> listMine() {
        UUID endUserId = AuthContext.actorIdOrNull();
        return endUserService.listOrdersForEndUser(endUserId)
                .stream()
                .map(o -> ResponseMapper.toDto(o, endUserService.computeProgress(o)))
                .toList();
    }

    @GetMapping("/{orderId}")
    public OrderDtos.OrderResponse getMine(@PathVariable UUID orderId) {
        UUID endUserId = AuthContext.actorIdOrNull();
        var order = endUserService.getOrderForEndUser(endUserId, orderId);
        return ResponseMapper.toDto(order, endUserService.computeProgress(order));
    }
}
