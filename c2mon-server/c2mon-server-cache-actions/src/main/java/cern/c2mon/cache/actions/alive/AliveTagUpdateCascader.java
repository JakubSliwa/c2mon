package cern.c2mon.cache.actions.alive;

import cern.c2mon.cache.actions.commfault.CommFaultService;
import cern.c2mon.cache.actions.state.SupervisionStateTagEvaluator;
import cern.c2mon.cache.actions.state.SupervisionStateTagService;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.cache.api.listener.CacheListener;
import cern.c2mon.server.common.alive.AliveTag;
import cern.c2mon.shared.common.CacheEvent;
import cern.c2mon.shared.common.supervision.SupervisionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Objects;

/**
 * Cascades the events received in the AliveTag cache to the CommFault
 * and SupervisionState caches. Applies sanitation and validation before
 * sending the event for performance reasons
 */
@Service
@Slf4j
public class AliveTagUpdateCascader implements CacheListener<AliveTag> {

  private final CommFaultService commFaultService;
  private final SupervisionStateTagService stateTagService;
  private C2monCache<AliveTag> aliveTagCache;

  @Inject
  public AliveTagUpdateCascader(C2monCache<AliveTag> aliveTagCache,
                                CommFaultService commFaultService,
                                SupervisionStateTagService stateTagService) {
    this.aliveTagCache = aliveTagCache;
    this.commFaultService = commFaultService;
    this.stateTagService = stateTagService;
  }

  private static boolean canUpdateCommFault(AliveTag aliveTag) {
    if (aliveTag.getCommFaultTagId() == null) {
      log.debug("Tag {} has no CommFaultTag ID - no CommFaultTag will be updated.", aliveTag.getName());
      return false;
    }
    if (aliveTag.getValue() == null) {
      log.warn("Tag {} has no value - no CommFaultTag will be updated.", aliveTag.getName());
      return false;
    }
    return true;
  }

  @PostConstruct
  public void register() {
    aliveTagCache.getCacheListenerManager().registerListener(this, CacheEvent.UPDATE_ACCEPTED);
  }

  @Override
  public void apply(AliveTag aliveTag) {
    Objects.requireNonNull(aliveTag, "Alive Tag received in cascading should never be null!");

    if (aliveTag.getSupervisedEntity() == null) {
      log.warn("AliveTag {} does not have a valid SupervisedEntity assigned ", aliveTag.getName());
      return;
    }

    if (aliveTag.getSupervisedEntity() == SupervisionEntity.PROCESS
      && SupervisionStateTagEvaluator.controlTagCanUpdateState(aliveTag.getStateTagId(), aliveTag)) {
      stateTagService.updateBasedOnControl(aliveTag.getStateTagId(), aliveTag);
    } else if (canUpdateCommFault(aliveTag)) {
      commFaultService.updateBasedOnAliveTimer(aliveTag);
    }
  }
}
