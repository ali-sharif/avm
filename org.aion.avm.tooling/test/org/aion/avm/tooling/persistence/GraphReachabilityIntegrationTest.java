package org.aion.avm.tooling.persistence;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.BillingRules;
import org.aion.avm.core.InstrumentationBasedStorageFees;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.tooling.AvmRule;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.tooling.abi.ABICompiler;
import org.aion.kernel.*;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;


/**
 * Tests some of the corner cases and other complex examples of graph reachability.
 * NOTE:  These tests very precisely measure the billing costs so changes to the fee schedule are likely to require updating these.
 * It may be worth relying on some more coarse-grained information, should it become available.
 */
public class GraphReachabilityIntegrationTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    private Address deployer = avmRule.getPreminedAccount();

    // added for methods with void return
    private static long byteArrayReturnCost = 600L;
    /**
     * Tests that a hidden object, changed via a path that is destroyed, is still observed as changed by other paths.
     * This version of the test calls in a sequence of transactions, meaning normal serialization.
     */
    @Test
    public void test249_direct() throws Exception {
        Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        Address contractAddr = doInitialDeploymentAndSetup(block);
        
        // Verify before.
        callStatic(block, contractAddr, getCost_check249(true), "check249", 4);
        
        // Run test.
        long modify_basicCost = 21708L;
        long modify_miscCharges = 236L + 300L + 100L + 600L + 37234L + 75L + 55L + 98L;
        long modify_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instances (3 - only 2 were actually modified)
                    + (2 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                ;
        callStatic(block, contractAddr, modify_basicCost + modify_miscCharges + modify_storageCharges + byteArrayReturnCost, "modify249");
        
        // Verify after.
        callStatic(block, contractAddr, getCost_check249(false), "check249", 5);
    }

    /**
     * Tests that a hidden object, changed via a path that is destroyed, is still observed as changed by other paths.
     * This version of the test calls in one transaction, using reentrant calls to modify the result.
     * This version only loads the changed object after the change.
     */
    @Test
    public void test249_notLoaded() throws Exception {
        Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        Address contractAddr = doInitialDeploymentAndSetup(block);
        
        // Verify before.
        callStatic(block, contractAddr, getCost_check249(true), "check249", 4);
        
        // Run test.
        long run_basicCost = 22796L;
        long run_miscCharges = 0L
                + 236L + 300L + 100L + 600L + 37234L + 135L + 327L + 100L + 17372L + 600L + 100L + 600L + 600L + 100L
                + 236L + 300L + 100L + 600L + 37234L + 75L + 55L + 98L
                + 100L + 108L + 100L + 50L + 55L + 50L
                ;
        long run_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instance
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 17L)
                // (heap) read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // (heap) read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // (heap) write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // (heap) write instances (3 - only 2 modified)
                    + (2 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                // read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instances (3 - we only see 1 from the callee, here)
                // TODO: This accounting can be fixed by issue-296.
                    + (1 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                // write instance
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 17L)
                ;
        // added byteArrayReturnCost cost for 3 methods with void return type
        callStatic(block, contractAddr, run_basicCost + run_miscCharges + run_storageCharges + byteArrayReturnCost * 3, "run249_reentrant_notLoaded");
        
        // Verify after.
        callStatic(block, contractAddr, getCost_check249(false), "check249", 5);
    }

    /**
     * Tests that a hidden object, changed via a path that is destroyed, is still observed as changed by other paths.
     * This version of the test calls in one transaction, using reentrant calls to modify the result.
     * This version loads the object before it is changed, then verifies the value is different after the change.
     */
    @Test
    public void test249_loaded() throws Exception {
        Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        Address contractAddr = doInitialDeploymentAndSetup(block);
        
        // Verify before.
        callStatic(block, contractAddr, getCost_check249(true), "check249", 4);
        
        // Run test.
        long run_basicCost = 22604L;
        long run_miscCharges = 0L
            + 236L + 300L + 100L + 600L + 37234L + 75L + 55L + 135L + 327L + 100L + 17372L + 600L + 100L + 600L + 600L + 100L
            + 236L + 300L + 100L + 600L + 37234L + 75L + 55L + 98L
            + 100L + 108L + 100L + 50L + 55L + 50L
            ;
        long run_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // read instance
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 17L)
                // (heap) read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // (heap) read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // (heap) write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // (heap) write instances (3 - only 2 modified)
                    + (2 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instances (3 - we only see 1 from the callee, here)
                // TODO: This accounting can be fixed by issue-296.
                    + (1 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                // write instance
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 17L)
                ;
        callStatic(block, contractAddr, run_basicCost + run_miscCharges + run_storageCharges + byteArrayReturnCost * 3, "run249_reentrant_loaded");
        
        // Verify after.
        callStatic(block, contractAddr, getCost_check249(false), "check249", 5);
    }

    /**
     * Tests that we can create a new instance, reference it from an existing object, but sever the path to it.
     * This should write-back the new instance so we should be able to find it, later.
     */
    @Test
    public void testNewObjectWritebackViaUnreachablePath() throws Exception {
        Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        Address contractAddr = doInitialDeploymentAndSetup(block);
        
        // Run test.
        long run_basicCost = 22668L;
        long run_miscCharges = 0L
                + 236L + 300L + 100L + 600L + 37234L + 327L + 100L + 17372L + 600L + 100L + 600L + 600L + 100L
                + 236L + 300L + 100L + 600L + 37234L + 252L + 131L
                + 100L + 108L + 100L + 50L
                ;
        long run_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instance
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 25L)
                // (heap) read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // (heap) read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // (heap) write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // (heap) write instances (4 = 3 + new 1)
                // (note that only 2 existing instances were modified)
                    + (2 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L)
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instance
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 25L)
                ;
        callStatic(block, contractAddr, run_basicCost + run_miscCharges + run_storageCharges + byteArrayReturnCost * 3, "runNewInstance_reentrant");
        
        // Verify result.
        long check_basicCost = 22156L;
        long check_miscCharges = 0L + 236L + 300L + 100L + 600L + 37234L + 80L + 600L;
        long check_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instances (4)
                    + (4 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instances (4)
                //    + (4 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                ;
        int value = (Integer) callStatic(block, contractAddr, check_basicCost + check_miscCharges + check_storageCharges, "checkNewInstance");
        Assert.assertEquals(5, value);
    }

    /**
     * Same as above but adds another level to the call stack to make sure we hit this case in both the graph processors.
     */
    @Test
    public void testNewObjectWritebackViaUnreachablePath2() throws Exception {
        Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        Address contractAddr = doInitialDeploymentAndSetup(block);
        
        // Run test.
        long run_basicCost = 22732L;
        long run_miscCharges = 0L
                + 236L + 300L + 100L + 600L + 37234L + 327L + 100L + 17372L + 600L + 100L + 600L + 600L + 100L
                + 236L + 300L + 100L + 600L + 37234L + 327L + 100L + 17372L + 600L + 100L + 600L + 600L + 100L
                + 236L + 300L + 100L + 600L + 37234L + 252L + 131L
                + 100L + 108L + 100L + 50L
                + 100L + 108L + 100L + 50L
                ;
        long run_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instance
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 32L)
                // (heap) read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // (heap) read instance
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 25L)
                // (heap) read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // (heap) read instances (3)
                    + (3 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // (heap) write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // (heap) write instances (4 = 3 + new 1)
                // (note that only 2 existing instances were modified)
                    + (2 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L)
                // (heap) write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // (heap) write instance
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 25L)
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instance
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 32L)
                ;
        callStatic(block, contractAddr, run_basicCost + run_miscCharges + run_storageCharges + byteArrayReturnCost * 5, "runNewInstance_reentrant2");
        
        // Verify result.
        long check_basicCost = 22156L;
        long check_miscCharges = 236L + 300L + 100L + 600L + 37234L + 80L + 600L;
        long check_storageCharges = 0L
                // read static
                    + (InstrumentationBasedStorageFees.FIXED_READ_COST + 161L)
                // read instances (4)
                    + (4 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instances (4)
                //    + (4 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                ;
        int value = (Integer) callStatic(block, contractAddr, check_basicCost + check_miscCharges + check_storageCharges, "checkNewInstance");
        Assert.assertEquals(5, value);
    }

    /**
     * Runs the setup routine, a few times, and verifies the expected GC behaviour after each.
     */
    @Test
    public void testVerifyGcCost() throws Exception {
        Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        Address contractAddr = doInitialDeploymentAndSetup(block);
        
        // GC now should reclaim nothing.
        AvmTransactionResult gcResult = (AvmTransactionResult) runGc(block, contractAddr);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, gcResult.getResultCode());
        Assert.assertEquals(0L, gcResult.getEnergyUsed());
        Assert.assertEquals(0L, gcResult.getEnergyRemaining());
        
        // Run the setup again and GC (should reclaim 5).
        callStatic(block, contractAddr, getCost_setup249(), "setup249");
        gcResult = (AvmTransactionResult) runGc(block, contractAddr);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, gcResult.getResultCode());
        Assert.assertEquals(-5 * InstrumentationBasedStorageFees.DEPOSIT_WRITE_COST, gcResult.getEnergyUsed());
        Assert.assertEquals(-(-5 * InstrumentationBasedStorageFees.DEPOSIT_WRITE_COST), gcResult.getEnergyRemaining());
        
        // GC now should reclaim nothing.
        gcResult = (AvmTransactionResult) runGc(block, contractAddr);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, gcResult.getResultCode());
        Assert.assertEquals(0L, gcResult.getEnergyUsed());
        Assert.assertEquals(0L, gcResult.getEnergyRemaining());
        
        // Run the setup again and GC (should reclaim 5).
        callStatic(block, contractAddr, getCost_setup249(), "setup249");
        gcResult = (AvmTransactionResult) runGc(block, contractAddr);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, gcResult.getResultCode());
        Assert.assertEquals(-5 * InstrumentationBasedStorageFees.DEPOSIT_WRITE_COST, gcResult.getEnergyUsed());
        Assert.assertEquals(-(-5 * InstrumentationBasedStorageFees.DEPOSIT_WRITE_COST), gcResult.getEnergyRemaining());
    }


    private Address doInitialDeploymentAndSetup(Block block) {
        // The assertions in this method depends on the gas charged, which in turn depends on the exact size of the jar file.
        // The AvmRule invokes the ABICompiler on all input jars.
        // As a result, we have to run the ABICompiler on the input jar to get the correct expected gas values.
        ABICompiler compiler = new ABICompiler();
        compiler.compile(JarBuilder.buildJarForMainAndClasses(GraphReachabilityIntegrationTestTarget.class));
        byte[] jar = compiler.getJarFileBytes();
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // Deploy.
        long energyLimit = 1_000_000l;
        long energyPrice = 1l;

        AvmTransactionResult createResult = (AvmTransactionResult) avmRule.deploy(deployer, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, createResult.getResultCode());
        Address contractAddr = new Address(createResult.getReturnData());
        
        // Check that the deployment cost is what we expected.
        // The first three numbers here are: basic cost of tx, processing cost and storage cost
        long basicCost = BillingRules.getBasicTransactionCost(txData);
        long codeInstantiationOfDeploymentFee = BillingRules.getDeploymentFee(1, jar.length);
        long codeStorageOfDeploymentFee = BillingRules.getCodeStorageFee(jar.length);
        long miscCharges = basicCost + codeInstantiationOfDeploymentFee + codeStorageOfDeploymentFee + 375L + 300L + 1500L + 6L + 53L;
        long storageCharges = 0L
                // static
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 161L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 32L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 17L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 25L
                ;
        Assert.assertEquals(miscCharges + storageCharges, createResult.getEnergyUsed());
        Assert.assertEquals(energyLimit - (miscCharges + storageCharges), createResult.getEnergyRemaining());
        
        // Setup test.
        callStatic(block, contractAddr, getCost_setup249(), "setup249");
        return contractAddr;
    }

    private Object callStatic(Block block, Address contractAddr, long expectedCost, String methodName, Object... args) {
        long energyLimit = 1_000_000l;
        byte[] argData = ABIEncoder.encodeMethodArguments(methodName, args);
        AvmTransactionResult result = (AvmTransactionResult) avmRule.call(deployer, contractAddr, BigInteger.ZERO, argData, energyLimit, 1l).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result.getResultCode());
        Assert.assertEquals(expectedCost, result.getEnergyUsed());
        Assert.assertEquals(energyLimit - expectedCost, result.getEnergyRemaining());
        return ABIDecoder.decodeOneObject(result.getReturnData());
    }

    private static long getCost_check249(boolean before) {
        long basicCost = 21784L;
        long miscCharges = 236L + 300L + 100L + 600L + 37234L + 75L + 55L + 67L;
        // We end up with a slightly different cost before/after changes.
        if (before) {
            miscCharges += 48L + 79L + 50L;
        } else {
            miscCharges += 50L;
        }
        long storageCharges = 0L
                // read static
                    + InstrumentationBasedStorageFees.FIXED_READ_COST + 161L
                // read instances (5)
                    + (5 * (InstrumentationBasedStorageFees.FIXED_READ_COST + 40L))
                // write static
                //    + (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L)
                // write instances (5)
                //    + (5 * (InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 40L))
                ;
        return basicCost + miscCharges + storageCharges + byteArrayReturnCost;
    }

    private static long getCost_setup249() {
        long basicCost = 21644L;
        long miscCharges = 236L + 300L + 100L + 600L + 37234L + 973L + 131L + 131L + 131L + 131L + 131L;
        long storageCharges = 0L
                // read static
                    + InstrumentationBasedStorageFees.FIXED_READ_COST + 161L
                // write static
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_UPDATE + 161L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L
                // instance
                    + InstrumentationBasedStorageFees.PER_OBJECT_WRITE_NEW + 40L
                ;
        return basicCost + miscCharges + storageCharges + byteArrayReturnCost;
    }

    private TransactionResult runGc(Block block, Address contractAddr) {
        long energyLimit = 1_000_000l;
        long energyPrice = 1l;
        Transaction gc = Transaction.garbageCollect(org.aion.types.Address.wrap(contractAddr.unwrap()), avmRule.kernel.getNonce(org.aion.types.Address.wrap(contractAddr.unwrap())), energyLimit, energyPrice);
        TransactionResult gcResult = avmRule.avm.run(avmRule.kernel, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(gc, block)})[0].get();
        return gcResult;
    }
}
