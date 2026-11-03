package tw.saltedfriedchicken;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;


@Mod(SaltedFriedChickenDelight.MOD_ID)
public class SaltedFriedChickenDelight {
    public static final String MOD_ID = "sfcdelight";
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, MOD_ID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, PepperRefillRecipe.Serializer> PEPPER_REFILL_SERIALIZER = RECIPE_SERIALIZERS.register("pepper_refill", PepperRefillRecipe.Serializer::new);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredBlock<Block> PEPPER_JAR_BLOCK = BLOCKS.register("pepper_jar", () -> new PepperJarBlock(Block.Properties.of().strength(0.5f).noOcclusion()));
    public static final DeferredBlock<Block> FRYER = BLOCKS.register("frying_station", () -> new FryingStationBlock(Block.Properties.of().strength(3.0f)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FryingStationBlockEntity>> FRYER_ENTITY = BLOCK_ENTITIES.register("frying_station", () -> BlockEntityType.Builder.of(FryingStationBlockEntity::new, FRYER.get()).build(null));
    public static final DeferredBlock<Block> FRYING_BASKET_BLOCK = BLOCKS.register("frying_basket", () -> new FryingBasketBlock(Block.Properties.of().strength(1.0f).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FryingBasketBlockEntity>> FRYING_BASKET_ENTITY = BLOCK_ENTITIES.register("frying_basket", () -> BlockEntityType.Builder.of(FryingBasketBlockEntity::new, FRYING_BASKET_BLOCK.get()).build(null));
    public static final DeferredBlock<Block> DEOILER = BLOCKS.register("de_oiler", () -> new DeOilerBlock(Block.Properties.of().strength(2.5f).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeOilerBlockEntity>> DEOILER_ENTITY = BLOCK_ENTITIES.register("de_oiler", () -> BlockEntityType.Builder.of(DeOilerBlockEntity::new, DEOILER.get()).build(null));
    public static final DeferredBlock<Block> PAPER_BAG = BLOCKS.register("paper_bag", () -> new FriedPackagingBlock(Block.Properties.of().strength(0.5f).noOcclusion(), 5));
    public static final DeferredBlock<Block> PAPER_BOX = BLOCKS.register("paper_box", () -> new FriedPackagingBlock(Block.Properties.of().strength(0.5f).noOcclusion(), 10));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FriedPackagingBlockEntity>> PACKAGING_ENTITY = BLOCK_ENTITIES.register("fried_packaging", () -> BlockEntityType.Builder.of(FriedPackagingBlockEntity::new, PAPER_BAG.get(), PAPER_BOX.get()).build(null));
    public static final DeferredBlock<Block> CART = BLOCKS.register("fried_food_cart", () -> new FriedFoodCartBlock(Block.Properties.of().strength(2.5f).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FriedFoodCartBlockEntity>> CART_ENTITY = BLOCK_ENTITIES.register("fried_food_cart", () -> BlockEntityType.Builder.of(FriedFoodCartBlockEntity::new, CART.get()).build(null));
    public static final DeferredBlock<Block> BASIL_CROP = BLOCKS.register("basil_crop", () -> new HerbCropBlock(Block.Properties.of().noCollission().randomTicks().instabreak().noOcclusion(), true));
    public static final DeferredBlock<Block> GARLIC_CROP = BLOCKS.register("garlic_crop", () -> new HerbCropBlock(Block.Properties.of().noCollission().randomTicks().instabreak().noOcclusion(), false));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register("sfcdelight", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sfcdelight"))
            .icon(SaltedFriedChickenDelight::creativeTabIcon)
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());
    public static final DeferredItem<Item> BASIL_SEEDS = ITEMS.register("basil_seeds", () -> new ItemNameBlockItem(BASIL_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> BASIL = ITEMS.registerSimpleItem("basil", new Item.Properties());
    public static final DeferredItem<Item> GARLIC = ITEMS.registerSimpleItem("garlic", new Item.Properties());
    public static final DeferredItem<Item> GARLIC_CLOVE = ITEMS.register("garlic_clove", () -> new ItemNameBlockItem(GARLIC_CROP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PAPER_BAG_ITEM = ITEMS.registerSimpleBlockItem("paper_bag", PAPER_BAG);
    public static final DeferredItem<BlockItem> PAPER_BOX_ITEM = ITEMS.registerSimpleBlockItem("paper_box", PAPER_BOX);
    public static final DeferredItem<Item> OILY_WASTE_PAPER_BAG = ITEMS.registerSimpleItem("oily_waste_paper_bag", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> OILY_WASTE_PAPER_BOX = ITEMS.registerSimpleItem("oily_waste_paper_box", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> FILLED_PAPER_BAG = ITEMS.register("filled_paper_bag", () -> new FriedContainerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FILLED_PAPER_BOX = ITEMS.register("filled_paper_box", () -> new FriedContainerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> DEOILER_ITEM = ITEMS.registerSimpleBlockItem("de_oiler", DEOILER);
    public static final DeferredItem<Item> HOT_FOOD_BASKET = ITEMS.register("hot_food_basket", () -> new FriedContainerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> STEEL_SCOOP = ITEMS.registerSimpleItem("steel_scoop", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LOADED_STEEL_SCOOP = ITEMS.register("loaded_steel_scoop", () -> new FriedContainerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PEPPER_SHAKER = ITEMS.register("pepper_shaker", () -> new PepperJarBlockItem(PEPPER_JAR_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WHITE_PEPPER_POWDER = ITEMS.register("white_pepper_powder", () -> new WhitePepperItem(new Item.Properties()));
    public static final DeferredItem<Item> COOKING_OIL = ITEMS.registerSimpleItem("cooking_oil", new Item.Properties());
    public static final DeferredItem<BlockItem> FRYING_BASKET_ITEM = ITEMS.registerSimpleBlockItem("frying_basket", FRYING_BASKET_BLOCK);
    public static final DeferredItem<BlockItem> FRYER_ITEM = ITEMS.registerSimpleBlockItem("frying_station", FRYER);
    public static final DeferredItem<Item> KNIFE = ITEMS.register("bloodletting_knife", () ->
        new SwordItem(Tiers.IRON, new Item.Properties().durability(128).attributes(
            SwordItem.createAttributes(Tiers.IRON, 2, -2.4f)))); // 1 base + 2 tier + 2 bonus = 5
    public static final DeferredItem<Item> PIG_BLOOD = ITEMS.registerSimpleItem("raw_pig_blood", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> DUCK_BLOOD = ITEMS.registerSimpleItem("raw_duck_blood", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> SOY_SAUCE = ITEMS.registerSimpleItem("soy_sauce", new Item.Properties());
    public static final DeferredItem<Item> WHEAT_FLOUR = ITEMS.registerSimpleItem("wheat_flour", new Item.Properties());
    public static final DeferredItem<Item> DRIED_TOFU = ITEMS.registerSimpleItem("dried_tofu", new Item.Properties());
    public static final DeferredItem<Item> HUNDRED_LAYER_TOFU = ITEMS.registerSimpleItem("hundred_layer_tofu", new Item.Properties());
    public static final DeferredItem<Item> RICE_BLOOD_CAKE = ITEMS.registerSimpleItem("rice_blood_cake", new Item.Properties());
    public static final DeferredItem<Item> COAGULATED_DUCK_BLOOD = ITEMS.registerSimpleItem("coagulated_duck_blood", new Item.Properties());
    public static final DeferredItem<Item> FISH_PASTE = ITEMS.registerSimpleItem("fish_paste", new Item.Properties());
    public static final DeferredItem<Item> RAW_TEMPURA_FISH_CAKE = ITEMS.registerSimpleItem("raw_tempura_fish_cake", new Item.Properties());
    public static final DeferredItem<Item> BATTERED_SQUID_TENTACLES = ITEMS.registerSimpleItem("battered_squid_tentacles", new Item.Properties());
    public static final DeferredItem<Item> FRIED_SQUID_TENTACLES = ITEMS.registerSimpleItem("fried_squid_tentacles", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final DeferredItem<Item> FRIED_TEMPURA_FISH_CAKE = ITEMS.registerSimpleItem("fried_tempura_fish_cake", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.5f).build()));
    public static final DeferredItem<Item> FRIED_SQUID_BALL = ITEMS.registerSimpleItem("fried_squid_ball", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.5f).build()));
    public static final DeferredItem<Item> FRIED_FRIES = ITEMS.registerSimpleItem("fried_fries", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).build()));
    public static final DeferredItem<Item> FRIED_SWEET_POTATO_FRIES = ITEMS.registerSimpleItem("fried_sweet_potato_fries", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).build()));
    public static final DeferredItem<Item> FRIED_CHICKEN_DICE = ITEMS.registerSimpleItem("fried_chicken_dice", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build()));
    public static final DeferredItem<Item> FRIED_COD_DICE = ITEMS.registerSimpleItem("fried_cod_dice", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final DeferredItem<Item> FRIED_DRIED_TOFU = ITEMS.registerSimpleItem("fried_dried_tofu", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build()));
    public static final DeferredItem<Item> FRIED_HUNDRED_LAYER_TOFU = ITEMS.registerSimpleItem("fried_hundred_layer_tofu", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final DeferredItem<Item> FRIED_RICE_BLOOD_CAKE = ITEMS.registerSimpleItem("fried_rice_blood_cake", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final DeferredItem<Item> FRIED_DUCK_BLOOD = ITEMS.registerSimpleItem("fried_duck_blood", new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build()));
    public static final DeferredItem<Item> RAW_SQUID_BALL = ITEMS.registerSimpleItem("raw_squid_ball", new Item.Properties());
    public static final DeferredItem<Item> RAW_FRIES = ITEMS.registerSimpleItem("raw_fries", new Item.Properties());
    public static final DeferredItem<Item> RAW_SWEET_POTATO_FRIES = ITEMS.registerSimpleItem("raw_sweet_potato_fries", new Item.Properties());
    public static final DeferredItem<Item> RAW_CHICKEN_DICE = ITEMS.registerSimpleItem("raw_chicken_dice", new Item.Properties());
    public static final DeferredItem<Item> RAW_COD_DICE = ITEMS.registerSimpleItem("raw_cod_dice", new Item.Properties());

    public static final DeferredItem<Item> PLASTIC_MATERIAL = ITEMS.registerSimpleItem("plastic_material", new Item.Properties());
    public static final DeferredItem<Item> PLASTIC_FRAME = ITEMS.registerSimpleItem("plastic_frame", new Item.Properties());
    public static final DeferredItem<Item> IRON_TONGS = ITEMS.registerSimpleItem("iron_tongs", new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> CART_ITEM = ITEMS.registerSimpleBlockItem("fried_food_cart", CART);
    public static final DeferredItem<Item> PLASTIC_BASKET = ITEMS.register("plastic_basket", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> PLASTIC_BASKET_WHITE = ITEMS.register("plastic_basket_white", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_ORANGE = ITEMS.register("plastic_basket_orange", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_MAGENTA = ITEMS.register("plastic_basket_magenta", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_LIGHT_BLUE = ITEMS.register("plastic_basket_light_blue", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_YELLOW = ITEMS.register("plastic_basket_yellow", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_LIME = ITEMS.register("plastic_basket_lime", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_PINK = ITEMS.register("plastic_basket_pink", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_GRAY = ITEMS.register("plastic_basket_gray", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_LIGHT_GRAY = ITEMS.register("plastic_basket_light_gray", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_CYAN = ITEMS.register("plastic_basket_cyan", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_PURPLE = ITEMS.register("plastic_basket_purple", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_BLUE = ITEMS.register("plastic_basket_blue", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_BROWN = ITEMS.register("plastic_basket_brown", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_GREEN = ITEMS.register("plastic_basket_green", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_RED = ITEMS.register("plastic_basket_red", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BASKET_BLACK = ITEMS.register("plastic_basket_black", () -> new PlasticFoodBasketItem(new Item.Properties().stacksTo(1)));

    private static ItemStack creativeTabIcon() { return new ItemStack(FRYER_ITEM.get()); }

    public SaltedFriedChickenDelight(IEventBus modBus) {
        RECIPE_SERIALIZERS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!event.getSource().isDirect()) return; // only direct melee attacks
        if (!player.getMainHandItem().is(KNIFE.get())) return;
        Item reward;
        if (event.getEntity() instanceof Pig) reward = PIG_BLOOD.get();
        else if (event.getEntity() instanceof Chicken) reward = DUCK_BLOOD.get();
        else return;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.GLASS_BOTTLE)) {
                stack.shrink(1);
                ItemStack result = new ItemStack(reward);
                if (!player.getInventory().add(result)) player.drop(result, false);
                return;
            }
        }
    }
}
