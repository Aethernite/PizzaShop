package PizzaShop.Controller;

import PizzaShop.Entities.Account;
import PizzaShop.Entities.Order;
import PizzaShop.Repositories.AccountRepository;
import PizzaShop.Entities.Product;
import PizzaShop.Repositories.OrderRepository;
import PizzaShop.Repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SessionAttributes({"account","cart"})
@org.springframework.stereotype.Controller
public class Controller {
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;


    @Autowired
    public Controller(AccountRepository accountRepository, ProductRepository productRepository, OrderRepository orderRepository) {
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }


    @GetMapping("/")
    public ModelAndView index(ModelAndView modelAndView) {
        modelAndView.setViewName("base-layout");
        modelAndView.addObject("view", "views/index");
        return modelAndView;
    }

    @PostMapping("/")
    public String login(Account account, RedirectAttributes redirectAttributes){

        if (isValid(account)) return "redirect:/";
        List<Account> accounts = this.accountRepository.findAll();
        Account acc = accounts.stream()
                .filter(account1 -> account.getUsername().equals(account1.getUsername()))
                .findAny()
                .orElse(null);
        if(acc == null){
            return "redirect:/";
        }
        if(acc.getUsername().equals(account.getUsername()) && acc.getPassword().equals(account.getPassword())){
            redirectAttributes.addFlashAttribute("account", acc);
            redirectAttributes.addFlashAttribute("cart", new ArrayList<Product>());
            if(acc.isAdmin()){
                return "redirect:/orders";
            }
            return "redirect:/pizzas";
        }
            return "redirect:/";
    }

    @GetMapping("/logout")
        public String logout(SessionStatus sessionStatus){
        sessionStatus.setComplete();
         return "redirect:/";
        }

    @GetMapping("/register")
    public ModelAndView register(ModelAndView modelAndView){
        modelAndView.setViewName("base-layout");
        modelAndView.addObject("view", "views/register");
        return modelAndView;
    }

    @PostMapping("/register")
    public String create(Account account){
        if (isValid(account)) return "redirect:/register";
        accountRepository.saveAndFlush(account);
        return "redirect:/";
    }

    @GetMapping("/pizzas")
    public ModelAndView pizzas(ModelAndView modelAndView, @ModelAttribute("account") Account account){
        if(!account.isAdmin()) {
            List<Product> products = this.productRepository.findAllByTypeAndStatus("pizza","enabled");
            modelAndView.setViewName("base-layout");
            modelAndView.addObject("products", products);
            modelAndView.addObject("view", "views/pizzas");
        }
        return modelAndView;
    }

   @GetMapping("/drinks")
    public ModelAndView drinks(ModelAndView modelAndView, @ModelAttribute("account") Account account){
       if(!account.isAdmin()) {
           List<Product> products = this.productRepository.findAllByTypeAndStatus("drink","enabled");
           modelAndView.setViewName("base-layout");
           modelAndView.addObject("view", "views/drinks");
           modelAndView.addObject("products", products);
           modelAndView.addObject("view", "views/drinks");
       }
       return modelAndView;
   }


    @GetMapping("/cart")
    public ModelAndView cart(ModelAndView modelAndView,@ModelAttribute("cart") List<Product> cart,@ModelAttribute("account") Account account){
        if(!account.isAdmin()) {
            double total = getTotal(cart);
            modelAndView.setViewName("base-layout");
            modelAndView.addObject("view", "views/cart");
            modelAndView.addObject("total", total);
        }
        return modelAndView;
    }


    @PostMapping("/add/{id}")
    public String add(@PathVariable(value = "id") Integer id, @ModelAttribute("cart") List<Product> cart, RedirectAttributes redirectAttributes, @ModelAttribute("account") Account account){
        if(!account.isAdmin()) {
            Product product = this.productRepository.findById(id).get();
            cart.add(product);
            redirectAttributes.addFlashAttribute("cart", cart);

        }
        return "redirect:/cart";
    }



    @PostMapping("/remove/{id}")
    public String remove( @PathVariable(value="id") Integer id, @ModelAttribute("cart") List<Product> cart, RedirectAttributes redirectAttributes, @ModelAttribute("account") Account account){
        if(!account.isAdmin()) {
            cart = removeFromCart(cart, id);
            redirectAttributes.addFlashAttribute("cart", cart);
        }
        return "redirect:/cart";
    }

    @PostMapping("/createOrder")
    public String createOrder(@ModelAttribute("account") Account account, @ModelAttribute("cart") List<Product> cart, RedirectAttributes redirectAttributes){
        if(!account.isAdmin()) {
            if(!cart.isEmpty()) {
                String products = convertListToString(cart);
                Order order = new Order(account.getUsername(), products, account.getAddress(), account.getPhone(), getTotal(cart));
                this.orderRepository.saveAndFlush(order);
                cart = new ArrayList<Product>();
                redirectAttributes.addFlashAttribute("cart", cart);
            }
        }
        return "redirect:/cart";
    }

    @GetMapping("/orders")
    public ModelAndView orders(ModelAndView modelAndView, @ModelAttribute("account") Account account){
        if(account.isAdmin()) {
            List<Order> orders = this.orderRepository.findAllByStatus("not completed");
            modelAndView.setViewName("base-layout");
            modelAndView.addObject("view", "views/orders");
            modelAndView.addObject("orders", orders);
        }
        return modelAndView;
    }

    @PostMapping("/completeOrder/{id}")
    public String completeOrder(@PathVariable(value = "id") Integer id, @ModelAttribute("account") Account account){
        if(account.isAdmin()){
            Optional<Order> order = this.orderRepository.findById(id);
            Order obj = order.get();
            obj.setStatus("completed");
            this.orderRepository.save(obj);
        }
        return "redirect:/orders";
    }

    @GetMapping("/status")
    public ModelAndView status(ModelAndView modelAndView, @ModelAttribute("account") Account account){
        if(account.isAdmin()){
            List<Product> products = this.productRepository.findAll();
            modelAndView.setViewName("base-layout");
            modelAndView.addObject("view", "views/status");
            modelAndView.addObject("products", products);
        }
        return modelAndView;
    }

    @PostMapping("/disable/{id}")
    public String disable(@PathVariable(value ="id")Integer id, @ModelAttribute("account") Account account){
        if(account.isAdmin()){
            Optional<Product> product = this.productRepository.findById(id);
            Product obj = product.get();
            obj.setStatus("disabled");
            this.productRepository.save(obj);
        }
        return "redirect:/status";
    }

    @PostMapping("/enable/{id}")
    public String enable(@PathVariable(value ="id")Integer id, @ModelAttribute("account") Account account){
        if(account.isAdmin()){
            Optional<Product> product = this.productRepository.findById(id);
            Product obj = product.get();
            obj.setStatus("enabled");
            this.productRepository.save(obj);
        }
        return "redirect:/status";
    }


    @GetMapping("/removeprods")
    public ModelAndView removeprods(ModelAndView modelAndView, @ModelAttribute("account") Account account){
        if(account.isAdmin()){
            List<Product> products = this.productRepository.findAll();
            modelAndView.setViewName("base-layout");
            modelAndView.addObject("view","views/removeprods");
            modelAndView.addObject("products", products);
        }
        return modelAndView;
    }

    @PostMapping("/removeprods/{id}")
    public String removeproduct(@PathVariable(value="id") Integer id, @ModelAttribute("account") Account account){
        if(account.isAdmin()){
            this.productRepository.deleteById(id);
        }
        return "redirect:/removeprods";
    }

//Utils ================================================================================================================
    private String convertListToString(List<Product> products){
        StringBuilder sb = new StringBuilder();
        for(Product pr: products){
           sb.append(pr.getName() + " " + String.format("%.2f",pr.getPrice()) + "\n");
        }
        return sb.toString();
    }
    private List<Product> removeFromCart(List<Product> cart, Integer id){
        for(int i=0; i<cart.size(); i++){
           if(cart.get(i).getId().intValue() == id.intValue()){
               cart.remove(i);
               break;
           }
        }
        return cart;
    }

    private boolean isValid(Account account) {
        if(account.getUsername() == null || account.getPassword() == null || account.getUsername().trim().isEmpty() || account.getPassword().trim().isEmpty()){
            return true;
        }
        return false;
    }

    private double getTotal(List<Product> productList){
        double total = 0;
        if(productList==null){
            return total;
        }
        for(Product pr: productList){
            total+=pr.getPrice();
        }
        return total;
    }
}
