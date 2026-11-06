package com.realmlord.menu;

import com.realmlord.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The manor storehouse: a double chest's worth of slots looking onto one page of the much larger
 * store at a time. Buttons (sent as menu button clicks) turn the page, sort it all, or take or
 * put everything at once.
 */
public class StorehouseMenu extends ChestMenu {
    public static final int PAGE = 54;
    public static final int PREV = 0, NEXT = 1, SORT = 2, TAKE_ALL = 3, STORE_ALL = 4;

    private final Paged view;
    public final int pages;
    private final DataSlot page = DataSlot.standalone();
    private final Runnable onChange;

    /** Client side: an empty page; the server fills it in. */
    public StorehouseMenu(int id, Inventory inv, int pages) {
        this(id, inv, new SimpleContainer(PAGE * Math.max(1, pages)), () -> { });
    }

    public StorehouseMenu(int id, Inventory inv, SimpleContainer store, Runnable onChange) {
        this(id, inv, new Paged(store), onChange);
    }

    private StorehouseMenu(int id, Inventory inv, Paged view, Runnable onChange) {
        super(ModMenus.STOREHOUSE.get(), id, inv, view, 6);
        this.view = view;
        this.pages = Math.max(1, view.store.getContainerSize() / PAGE);
        this.onChange = onChange;
        addDataSlot(page);
    }

    public int page() {
        return page.get();
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        switch (id) {
            case PREV -> turn(-1);
            case NEXT -> turn(1);
            case SORT -> {
                Sorting.sort(view.store, 0, view.store.getContainerSize());
                onChange.run();
            }
            case TAKE_ALL -> {
                Sorting.takeAll(this, p);
                onChange.run();
            }
            case STORE_ALL -> {
                Sorting.storeAll(this, p);
                onChange.run();
            }
            default -> {
                return false;
            }
        }
        broadcastChanges();
        return true;
    }

    private void turn(int by) {
        int to = Math.floorMod(page.get() + by, pages);
        page.set(to);
        view.page = to;
    }

    /** One page of the whole store. */
    static final class Paged implements Container {
        final SimpleContainer store;
        int page;

        Paged(SimpleContainer store) {
            this.store = store;
        }

        private int at(int slot) {
            return page * PAGE + slot;
        }

        @Override
        public int getContainerSize() {
            return PAGE;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < PAGE; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return at(slot) < store.getContainerSize() ? store.getItem(at(slot)) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int n) {
            return at(slot) < store.getContainerSize() ? store.removeItem(at(slot), n) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return at(slot) < store.getContainerSize() ? store.removeItemNoUpdate(at(slot)) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack s) {
            if (at(slot) < store.getContainerSize()) store.setItem(at(slot), s);
        }

        @Override
        public void setChanged() {
            store.setChanged();
        }

        @Override
        public boolean stillValid(Player p) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < PAGE; i++) setItem(i, ItemStack.EMPTY);
        }
    }
}
