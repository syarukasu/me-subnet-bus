package com.syaru.mesubnetbus;

import appeng.api.config.IncludeExclude;
import appeng.api.storage.MEStorage;
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.NullInventory;
import appeng.util.prioritylist.IPartitionList;

/** Uses the same operation/filter implementation as AE2's StorageBusPart. */
final class SubnetStorage extends MEInventoryHandler {
    SubnetStorage() {
        super(NullInventory.of());
        setExtractFiltering(true, true);
    }

    void disconnect() {
        setDelegate(NullInventory.of());
    }

    void connect(BaseSubnetPart part, MEStorage inventory) {
        var filter = IPartitionList.builder();
        part.filters.forEach(filter::add);
        setPartitionList(filter.build());
        setWhitelist(part.excludeFilter ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
        setAllowInsertion(part.mode.insert);
        setAllowExtraction(part.mode.extract);
        setDelegate(inventory == null ? NullInventory.of() : inventory);
    }
}
