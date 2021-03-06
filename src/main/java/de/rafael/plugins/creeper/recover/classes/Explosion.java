/*
 * Copyright (c) 2022. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *         notice, this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *     * Neither the name of the developer nor the names of its contributors
 *         may be used to endorse or promote products derived from this software
 *         without specific prior written permission.
 *     * Redistributions in source or binary form must keep the original package
 *         and class name.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.rafael.plugins.creeper.recover.classes;

//------------------------------
//
// This class was developed by Rafael K.
// On 31.12.2021 at 12:37
// In the project CreeperRecover
//
//------------------------------

import de.rafael.plugins.creeper.recover.classes.data.ContainerItems;
import de.rafael.plugins.creeper.recover.CreeperRecover;
import de.rafael.plugins.creeper.recover.classes.data.SignColor;
import de.rafael.plugins.creeper.recover.classes.data.SignLines;
import de.rafael.plugins.creeper.recover.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.DoubleChestInventory;

import java.util.*;
import java.util.Comparator;

public class Explosion {

    private final Location location;
    private final List<ExplodedBlock> blocks = new ArrayList<>();

    public Explosion(Location location, List<Block> blocks) {
        this.location = location;
        List<Block> sortedBlocks = blocks.stream().sorted(Comparator.comparingDouble(item -> ((Block) item).getLocation().distance(location)).reversed()).toList();
        List<Block> ignoredBlocks = new ArrayList<>();
        for (Block block : sortedBlocks) {
            if (ignoredBlocks.stream().anyMatch(item -> item.getLocation().distance(block.getLocation()) < 0.1)) {
                continue;
            }
            if (block.getType() == Material.TNT) {
                TNTPrimed tnt = (TNTPrimed) Objects.requireNonNull(block.getLocation().getWorld()).spawnEntity(MathUtils.toCenterLocation(block.getLocation()), EntityType.PRIMED_TNT);
                tnt.setFuseTicks(MathUtils.generateRandomInteger(10, 30));
                tnt.setVelocity(MathUtils.calculateVectorBetween2Locations(MathUtils.toCenterLocation(location.clone()), MathUtils.toCenterLocation(block.getLocation().clone())).normalize().multiply(0.7));
                continue;
            }
            ExplodedBlock explodedBlock = new ExplodedBlock(block.getLocation().clone(), block.getType(), block.getBlockData().clone());
            if (block.getState() instanceof Container container) {
                ContainerItems inventory = new ContainerItems();
                for (int i = 0; i < container.getInventory().getStorageContents().length; i++) {
                    if (container.getInventory().getStorageContents()[i] != null) {
                        inventory.set(i, container.getInventory().getStorageContents()[i].clone());
                    }
                }
                explodedBlock.addData(inventory);
            }
            if (block.getState() instanceof Chest chest) {
                if (chest.getInventory() instanceof DoubleChestInventory doubleChestInventory) {
                    DoubleChest doubleChest = doubleChestInventory.getHolder();

                    assert doubleChest != null;
                    Chest leftSide = (Chest) doubleChest.getLeftSide();
                    Chest rightSide = (Chest) doubleChest.getRightSide();
                    assert leftSide != null;
                    assert rightSide != null;
                    if (block.getLocation().distance(leftSide.getBlock().getLocation()) < 0.1) {
                        ignoredBlocks.add(rightSide.getBlock());
                        ExplodedBlock extraChest = new ExplodedBlock(rightSide.getBlock().getLocation().clone(), rightSide.getBlock().getType(), rightSide.getBlock().getBlockData().clone());
                        explodedBlock.connectBlock(extraChest);
                    } else if (block.getLocation().distance(rightSide.getBlock().getLocation()) < 0.1) {
                        ignoredBlocks.add(leftSide.getBlock());
                        ExplodedBlock extraChest = new ExplodedBlock(leftSide.getBlock().getLocation().clone(), leftSide.getBlock().getType(), leftSide.getBlock().getBlockData().clone());
                        explodedBlock.connectBlock(extraChest);
                    }
                }
            }
            if(block.getState() instanceof Sign sign) {
                explodedBlock.addData(new SignLines(sign.getLines()));
                explodedBlock.addData(new SignColor(sign.getColor()));
            }
            this.blocks.add(explodedBlock);
        }
    }

    public void recoverBlock() {
        Iterator<ExplodedBlock> iterator = this.blocks.iterator();
        if (iterator.hasNext()) {
            ExplodedBlock block = iterator.next();
            block.recover();
            try {
                iterator.remove();
            } catch (ConcurrentModificationException ignored) {

            }
        } else {
            this.blocks.clear();
        }
    }

    public int recoverBlocks() {
        int recovered = 0;
        Iterator<ExplodedBlock> iterator = this.blocks.iterator();
        while (iterator.hasNext()) {
            ExplodedBlock block = iterator.next();
            block.recover();
            recovered++;
            iterator.remove();
        }
        return recovered;
    }

    public int recoverBlocks(int amount) {
        int recovered = 0;
        Iterator<ExplodedBlock> iterator = this.blocks.iterator();
        while (iterator.hasNext()) {
            if (recovered >= amount) {
                break;
            }
            ExplodedBlock block = iterator.next();
            block.recover();
            recovered++;
            iterator.remove();
        }
        return recovered;
    }

    public boolean isFinished() {
        return this.blocks.size() == 0;
    }

    public void finished() {
        CreeperRecover.getCreeperRecover().getPluginStats().explosionsRecovered();
    }

    public List<ExplodedBlock> getBlocks() {
        return blocks;
    }

    public Location getLocation() {
        return location;
    }

}
