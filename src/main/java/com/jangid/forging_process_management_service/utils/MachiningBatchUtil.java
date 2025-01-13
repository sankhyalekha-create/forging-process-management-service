package com.jangid.forging_process_management_service.utils;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;

import java.util.List;

public class MachiningBatchUtil {

  /**
   * Get the latest DailyMachiningBatch based on the ID field after sorting the list.
   *
   * @param machiningBatch MachiningBatch containing the list of DailyMachiningBatch entities.
   * @return The latest DailyMachiningBatch or null if the list is empty.
   */
  public static DailyMachiningBatch getLatestDailyMachiningBatch(MachiningBatch machiningBatch) {
    List<DailyMachiningBatch> dailyMachiningBatches = machiningBatch.getDailyMachiningBatch();

    if (dailyMachiningBatches == null || dailyMachiningBatches.isEmpty()) {
      return null; // Return null if there are no batches
    }

    // Sort the list based on ID in ascending order
    dailyMachiningBatches.sort((a, b) -> (int) (a.getId() - b.getId()));

    // Return the last element (latest batch based on ID)
    return dailyMachiningBatches.get(dailyMachiningBatches.size() - 1);
  }
}
