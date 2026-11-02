package tw.saltedfriedchicken;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.ItemLike;

public class HerbCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private final boolean basil;

    // Four stages: ages 0, 1, 2, and mature age 3.

    public HerbCropBlock(Properties properties, boolean basil) {
        super(properties);
        this.basil = basil;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override public IntegerProperty getAgeProperty() { return AGE; }
    @Override public int getMaxAge() { return 3; }
    @Override protected ItemLike getBaseSeedId() {
        return basil ? SaltedFriedChickenDelight.BASIL_SEEDS.get() : SaltedFriedChickenDelight.GARLIC_CLOVE.get();
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }
}
