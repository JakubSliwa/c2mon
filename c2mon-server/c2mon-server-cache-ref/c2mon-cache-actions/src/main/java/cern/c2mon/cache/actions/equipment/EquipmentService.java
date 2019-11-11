package cern.c2mon.cache.actions.equipment;

import cern.c2mon.cache.actions.BaseEquipmentServiceImpl;
import cern.c2mon.cache.actions.alivetimer.AliveTimerService;
import cern.c2mon.cache.actions.commfault.CommFaultService;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.server.common.datatag.DataTag;
import cern.c2mon.server.common.equipment.Equipment;
import cern.c2mon.shared.common.supervision.SupervisionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Szymon Halastra
 * @author Alexandros Papageorgiou Koufidis
 */
@Slf4j
@Service
public class EquipmentService extends BaseEquipmentServiceImpl<Equipment> implements EquipmentOperations {

  private final C2monCache<DataTag> dataTagCacheRef;
  private final CommFaultService commFaultService;

  @Inject
  public EquipmentService(final C2monCache<Equipment> equipmentCacheRef,
                          final C2monCache<DataTag> dataTagCacheRef,
                          final AliveTimerService aliveTimerService,
                          final CommFaultService commFaultService) {
    super(equipmentCacheRef, commFaultService.getCache(), aliveTimerService, SupervisionConstants.SupervisionEntity.EQUIPMENT);
    this.dataTagCacheRef = dataTagCacheRef;
    this.commFaultService = commFaultService;
  }

  @Override
  public Collection<Long> getEquipmentAlives() {
    return getCache().query(i -> true).stream().map(Equipment::getAliveTagId).collect(Collectors.toSet());
  }

  @Override
  public void addEquipmentToProcess(Long equipmentId, Long processId) {
    // TODO do these with Brice

  }

  @Override
  public void removeCommandFromEquipment(Long equipmentId, Long commandId) {

  }

  @Override
  public void addCommandToEquipment(Long equipmentId, Long commandId) {

  }
}
