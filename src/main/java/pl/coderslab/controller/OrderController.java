package pl.coderslab.controller;

import org.hibernate.Hibernate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pl.coderslab.Services.CurrentUser;
import pl.coderslab.entity.Material;
import pl.coderslab.entity.Order;
import pl.coderslab.entity.OrderItem;
import pl.coderslab.repository.CustomerRepository;
import pl.coderslab.repository.MaterialRepository;
import pl.coderslab.repository.OrderItemRepository;
import pl.coderslab.repository.OrderRepository;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/order")
public class OrderController {
    private OrderRepository orderRepository;
    private MaterialRepository materialRepository;
    private CustomerRepository customerRepository;
    private OrderItemRepository orderItemRepository;

    public OrderController(OrderRepository orderRepository, MaterialRepository materialRepository, CustomerRepository customerRepository,OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.materialRepository = materialRepository;
        this.customerRepository = customerRepository;
        this.orderItemRepository = orderItemRepository;
    }




    @GetMapping("/orderList")
    public String orderList(HttpServletRequest request, @AuthenticationPrincipal CurrentUser customUser){
        List<Order> orders = orderRepository.getAllByCustomer(customUser.getUser());
        for(Order o:orders){
            Hibernate.initialize(o.getItems());
        }
        request.setAttribute("orders", orders);
        return "order/list";
    }
    @GetMapping("/adminList")
    public String adminList(HttpServletRequest request, @AuthenticationPrincipal CurrentUser customUser){
        List<Order> orders = orderRepository.findAll();
        for(Order o:orders){
            Hibernate.initialize(o.getItems());
        }
        request.setAttribute("orders", orders);
        return "order/list";
    }

    @GetMapping("/editOrder/{id}")
    public String addItem(@PathVariable Long id, Model model){
        Order order = orderRepository.getOne(id);
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        model.addAttribute("orderItem", orderItem);
        return "order/itemForm";
    }
    @Transactional
    @PostMapping("/editOrder")
    public String addItemPost(@Valid OrderItem item, BindingResult result){
        if(result.hasErrors()){
            return "redirect:../order/error";
        }
        item.setSize(item.getWidth() * item.getLength() * item.getQuantity());
        Long materialId = item.getMaterialId();
        Optional<Material> optional = materialRepository.findById(materialId);
        Material material = optional.get();
        double price = material.getPrice() * item.getSize();
        double priceVat = Math.round(price * 100) / 100;
        item.setPrice(price);
        item.setPriceWithVAT(priceVat);
        orderItemRepository.save(item);
        return "redirect:../order/orderList";
    }


    @GetMapping("/addOrder")
    public String addOrder(Model model){
        model.addAttribute("order", new Order());
        return "order/orderForm";
    }
    @Transactional
    @PostMapping("/addOrder")
    public String addOrderPost(@Valid Order order, BindingResult result, @AuthenticationPrincipal CurrentUser customUser){
        if(result.hasErrors()){
            return "redirect:../order/error";
        }
        order.setCustomer(customUser.getUser());
        order.setAccepted(false);
    orderRepository.save(order);
    return "redirect:../order/orderList";
    }

    @Transactional
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id){
        Order toDel = orderRepository.getOne(id);
        orderRepository.delete(toDel);
        return "redirect:../../order/adminList";
    }


    @Transactional
    @GetMapping("/userDelete/{id}")
    public String userDelete(@PathVariable Long id){
        Order toDel = orderRepository.getOne(id);
        if(toDel.getAccepted()==true) {
            return "order/cantDel";
        }else{
            orderRepository.delete(toDel);
            return "redirect:../../order/orderList";
        }
    }


//    @Transactional
//    @GetMapping("/delete/item/{id}")
//    public String delItem(@PathVariable Long id){
//        OrderItem toDel = orderItemRepository.getOne(id);
//        Order order = orderRepository.getByItems(toDel);
//        Boolean accepted = order.getAccepted();
//        if(accepted.equals(true)){
//            return "redirect:../../cantDel";
//        }
//        order.getItems().remove(toDel);
//        orderRepository.save(order);
//        orderItemRepository.delete(toDel);
//        return "redirect:../../orderList";
//    }

    @Transactional
    @GetMapping("/accept/{id}")
    public String acceptPost(@PathVariable Long id){
        Order toUpdate = orderRepository.getOne(id);
        toUpdate.setAccepted(true);
        orderRepository.save(toUpdate);
        return "redirect:../../order/adminList";
    }

    @GetMapping("/error")
    public String error(){
        return "order/invalidData";
    }
    @GetMapping("/cantDel")
    public String cannot(){
        return "order/cantDel";
    }

    @ModelAttribute("materials")
    public List<Material> materials(){
        return materialRepository.findAll();
    }


}
