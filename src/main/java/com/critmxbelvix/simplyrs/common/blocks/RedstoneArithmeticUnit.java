package com.critmxbelvix.simplyrs.common.blocks;

import com.critmxbelvix.simplyrs.common.blocks.srsvoxelshapes.SRSVoxelShapes;
import com.critmxbelvix.simplyrs.common.creativetabs.SimplyRSCreativeTab;
import com.critmxbelvix.simplyrs.common.items.RedstoneWrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;

import static java.util.Collections.singletonList;

public class RedstoneArithmeticUnit extends Block {
    private static final Logger LOGGER = LogManager.getLogger();
    final static String name = "redstone_arithmetic_unit";
    final static CreativeModeTab tab = SimplyRSCreativeTab.SRS_TAB;
    final static Properties arithmetic_unit_properties = Properties.of(Material.STONE).strength(0.3f).dynamicShape();
    public static final EnumProperty<ArithmeticModes> MODE = EnumProperty.create("mode", ArithmeticModes.class, ArithmeticModes.values());
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public RedstoneArithmeticUnit(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(MODE, ArithmeticModes.ADD)
        );
    }

    public static String m_getName()
    {
        return name;
    }
    public static CreativeModeTab m_getTab()
    {
        return tab;
    }
    public static Properties m_getProperties()
    {
        return arithmetic_unit_properties;
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canSupportRigidBlock(pLevel, pPos.below());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SRSVoxelShapes.ARITHMETIC_UNIT_SHAPE;
    }

    // Blockstates

    public BlockState getStateForPlacement(BlockPlaceContext pContext)
    {
        BlockState blockstate = super.getStateForPlacement(pContext);
        Direction direction = pContext.getHorizontalDirection();
        pContext.getLevel().scheduleTick(pContext.getClickedPos(),blockstate.getBlock(),1);

        return this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(MODE, ArithmeticModes.ADD)
                .setValue(POWERED,shouldTurnOn(pContext.getLevel(),pContext.getClickedPos(),blockstate));
    }


    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder){
        pBuilder.add(POWERED,FACING, MODE);
    }

    public BlockState rotate(BlockState pState, Rotation pRotation){
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror){
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    // Block Drops

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {

        List<ItemStack> drops = super.getDrops(pState, pBuilder);
        if (!drops.isEmpty())
            return drops;
        return singletonList(new ItemStack(this, 1));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(pPlayer.getItemInHand(pHand).getItem() instanceof RedstoneWrench){
            ArithmeticModes mode = pState.getValue(MODE);
            ArithmeticModes newMode;
            newMode = switch(mode){
                case ADD -> ArithmeticModes.SUBTRACT;
                case SUBTRACT -> ArithmeticModes.MULTIPLY;
                case MULTIPLY -> ArithmeticModes.DIVIDE;
                case DIVIDE -> ArithmeticModes.ADD;
            };
            pLevel.setBlockAndUpdate(pPos,pState.setValue(MODE,newMode));
            LOGGER.info("Used");
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    enum ArithmeticModes implements StringRepresentable {
        ADD("add"),
        SUBTRACT("subtract"),
        MULTIPLY("multiply"),
        DIVIDE("divide");

        private final String name;

        ArithmeticModes(String pName){
            this.name = pName;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    // Redstone

    public boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(FACING).getOpposite() == pSide ? this.getOutputSignal(pBlockAccess, pPos, pBlockState) : 0;
    }
    @Override
    public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getSignal(pBlockAccess, pPos, pSide);
    }

    protected boolean shouldTurnOn(Level pLevel, BlockPos pPos, BlockState pState) { return true; }

    protected int getInputSignalAt(BlockGetter pLevel, BlockPos pPos, Direction pSide) {
        BlockState blockstate = pLevel.getBlockState(pPos);

        if (this.isSideInput(blockstate)) {
            if (blockstate.is(Blocks.REDSTONE_BLOCK)) {
                return 15;
            } else {
                return blockstate.is(Blocks.REDSTONE_WIRE) ? blockstate.getValue(RedStoneWireBlock.POWER) : blockstate.getSignal(pLevel, pPos, pSide);
            }
        } else {
            return 0;
        }
    }

    protected boolean isSideInput(BlockState pState) {
        return pState.isSignalSource();
    }

    protected int getOutputSignal(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        ArithmeticModes mode = pState.getValue(MODE);
        Direction direction = pState.getValue(FACING);
        int a = getInputSignalAt(pLevel,pPos,direction.getCounterClockWise());
        int b = getInputSignalAt(pLevel,pPos,direction.getOpposite());
        int c = getInputSignalAt(pLevel,pPos,direction.getClockWise());

        return switch (mode) {
            case ADD -> Math.min((a + b + c), 15);

            case SUBTRACT -> (a - b - c) >= 0 ? (a - b - c) : -(a - b - c);

            case MULTIPLY -> Math.min((a * b * c), 15);

            case DIVIDE -> a!=0 && b!=0 && c!=0 ? a/b/c : 0;
        };
    }

    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, Random pRand) {
        boolean flag = pState.getValue(POWERED);
        boolean flag1 = this.shouldTurnOn(pLevel, pPos, pState);
        if (flag && !flag1) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(false)), 2);
        } else if(flag1){
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(true)), 2);
        }
        pLevel.neighborChanged(pPos.relative(pState.getValue(FACING)),this,pPos);
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {

        Direction north = pState.getValue(FACING);
        Direction south = north.getOpposite();
        Direction east = north.getClockWise();
        Direction west = north.getCounterClockWise();

        if(!pState.canSurvive(pLevel,pPos)){
            BlockEntity blockentity = pState.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
            dropResources(pState, pLevel, pPos, blockentity);
            pLevel.removeBlock(pPos, false);
        }
        else {
            BlockState blockstate = pLevel.getBlockState(pPos)
                    .setValue(POWERED,getInputSignalAt(pLevel,pPos,east) > 0 ||
                                    getInputSignalAt(pLevel,pPos,west) > 0 ||
                                    getInputSignalAt(pLevel,pPos,south) > 0
                    );
            pLevel.setBlockAndUpdate(pPos, blockstate);
            pLevel.scheduleTick(pPos, this, 1, TickPriority.VERY_HIGH);
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            pLevel.neighborChanged(pPos.relative(pState.getValue(FACING)),this, pPos);
        }
    }


}