package cern.c2mon.cache.actions.tag;

import cern.c2mon.cache.actions.BaseEquipmentServiceImpl;
import cern.c2mon.cache.api.factory.AbstractCacheObjectFactory;
import cern.c2mon.server.common.control.ControlTag;
import cern.c2mon.server.common.datatag.DataTag;
import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.common.tag.AbstractTagCacheObject;
import cern.c2mon.server.common.tag.Tag;
import cern.c2mon.server.common.util.MetadataUtils;
import cern.c2mon.shared.common.ConfigurationException;
import cern.c2mon.shared.common.PropertiesAccessor;
import cern.c2mon.shared.common.SimpleTypeReflectionHandler;
import cern.c2mon.shared.common.datatag.DataTagAddress;
import cern.c2mon.shared.common.datatag.DataTagDeadband;
import cern.c2mon.shared.common.type.TypeConverter;
import cern.c2mon.shared.common.validation.MicroValidator;
import cern.c2mon.shared.daq.config.Change;
import cern.c2mon.shared.daq.config.DataTagAddressUpdate;
import cern.c2mon.shared.daq.config.DataTagUpdate;
import cern.c2mon.shared.daq.config.HardwareAddressUpdate;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Properties;

import static cern.c2mon.shared.common.datatag.DataTagConstants.MODE_OPERATIONAL;
import static cern.c2mon.shared.common.datatag.DataTagConstants.MODE_TEST;

/**
 * Creates {@link DataTag} cache object
 * {@link DataTag} contains common interface for DataTag and ControlTag
 * apparently they are differ only by class name, methods definitions are the same
 *
 * @author Szymon Halastra, Alexandros Papageorgiou
 */
@Slf4j
public abstract class TagCacheObjectFactory<T extends Tag> extends AbstractCacheObjectFactory<T> {

  private final BaseEquipmentServiceImpl coreAbstractEquipmentService;

  public TagCacheObjectFactory(BaseEquipmentServiceImpl coreAbstractEquipmentService) {
    this.coreAbstractEquipmentService = coreAbstractEquipmentService;
  }

  @Override
  public Change configureCacheObject(T tag, Properties properties) throws ConfigurationException, IllegalArgumentException, IllegalAccessException {
    DataTagCacheObject dataTagCacheObject = (DataTagCacheObject) tag;
    DataTagUpdate dataTagUpdate = setCommonProperties(dataTagCacheObject, properties);

    // TAG equipment identifier
    // Ignore the equipment id for control tags as control tags are INDIRECTLY
    // referenced via the equipment's aliveTag and commFaultTag fields
    if (coreAbstractEquipmentService != null && !(dataTagCacheObject instanceof ControlTag)) {

      // Only one of equipment / subequipment Id should be set. But if both are there, we will overwrite the process ID
      // with the equipment (more important)
      new PropertiesAccessor(properties)
        .getLong("subEquipmentId").ifPresent(subEquipmentId -> {
        dataTagCacheObject.setSubEquipmentId(subEquipmentId);
        dataTagCacheObject.setProcessId(coreAbstractEquipmentService.getProcessIdForAbstractEquipment(subEquipmentId));
      }).getLong("equipmentId").ifPresent(equipmentId -> {
        dataTagCacheObject.setEquipmentId(equipmentId);
        dataTagCacheObject.setProcessId(coreAbstractEquipmentService.getProcessIdForAbstractEquipment(equipmentId));
      });
    }

    new PropertiesAccessor(properties)
      .getAs("minValue", prop -> (Comparable) TypeConverter.cast("null".equals(prop) ? null : prop, dataTagCacheObject.getDataType()))
      .ifPresent(minValue -> {
        dataTagCacheObject.setMinValue(minValue);
        dataTagUpdate.setMinValue((Number) minValue);
      }).getAs("maxValue", prop -> (Comparable) TypeConverter.cast("null".equals(prop) ? null : prop, dataTagCacheObject.getDataType()))
      .ifPresent(maxValue -> {
        dataTagCacheObject.setMaxValue(maxValue);
        dataTagUpdate.setMaxValue((Number) maxValue);
      }).getAs("address", DataTagAddress::fromConfigXML).ifPresent(dataTagAddress -> {
      dataTagCacheObject.setAddress(dataTagAddress);
      try {
        setUpdateDataTagAddress(dataTagAddress, dataTagUpdate);
      } catch (IllegalAccessException e) {
        log.debug("Failed to update datatag address ", e);
      }
    });

    if (dataTagCacheObject.getEquipmentId() != null)
      dataTagUpdate.setEquipmentId(dataTagCacheObject.getEquipmentId());

    return dataTagUpdate;
  }

// TODO (2019) This used to be in a transaction and seems like a hot cache object being edited. Investigate callers and restore/replace that behavior

  /**
   * Notice only non-null properties are set, the others staying unaffected
   * by this method.
   *
   * @return the returned update object with changes that need sending to the
   * DAQ (only used when reconfiguring a Data/ControlTag, not rules)
   * IMPORTANT: the change id and equipment id still needs setting on the returned object
   * in the DataTag-specific facade
   * @throws ConfigurationException
   */
  protected DataTagUpdate setCommonProperties(AbstractTagCacheObject tag, Properties properties)
    throws ConfigurationException {

    DataTagUpdate innerDataTagUpdate = new DataTagUpdate();
    innerDataTagUpdate.setDataTagId(tag.getId());

    new PropertiesAccessor(properties)
      .getString("name").ifPresent(name -> {
      tag.setName(name);
      innerDataTagUpdate.setName(name);
    }).getString("description").ifPresent(description -> {
      tag.setDescription(description);
      innerDataTagUpdate.setName(description);
    }).getString("dataType").ifPresent(dataType -> {
      tag.setDataType(dataType);
      innerDataTagUpdate.setDataType(dataType);
    }).getShort("mode").ifPresent(mode -> {
      tag.setMode(mode);
      innerDataTagUpdate.setMode(mode);
    }).getString("isLogged").ifPresent(isLogged -> tag.setLogged(isLogged.equalsIgnoreCase("true")))
      .getString("unit").ifPresent(unit -> tag.setUnit(checkAndSetNull(unit)))
      .getString("dipAddress").ifPresent(dipAddress -> tag.setDipAddress(checkAndSetNull(dipAddress)))
      .getString("japcAddress").ifPresent(japcAddress -> tag.setJapcAddress(checkAndSetNull(japcAddress)));

    cern.c2mon.server.common.metadata.Metadata newMetadata = MetadataUtils.parseMetadataConfiguration(properties, tag.getMetadata());
    tag.setMetadata(newMetadata);

    return innerDataTagUpdate;
  }

  /**
   * Sets the DataTagAddress part of an update from the XML String.
   *
   * @param dataTagAddress the new address
   * @param dataTagUpdate  the update object for which the address needs setting
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   */
  protected void setUpdateDataTagAddress(final DataTagAddress dataTagAddress, final DataTagUpdate dataTagUpdate) throws IllegalArgumentException, IllegalAccessException {
    DataTagAddressUpdate dataTagAddressUpdate = new DataTagAddressUpdate();
    dataTagUpdate.setDataTagAddressUpdate(dataTagAddressUpdate);
    dataTagAddressUpdate.setGuaranteedDelivery(dataTagAddress.isGuaranteedDelivery());
    dataTagAddressUpdate.setPriority(dataTagAddress.getPriority());
    if (dataTagAddress.getTimeToLive() != DataTagAddress.TTL_FOREVER) {
      dataTagAddressUpdate.setTimeToLive(dataTagAddress.getTimeToLive());
    }
    if (dataTagAddress.getValueDeadbandType() != DataTagDeadband.DEADBAND_NONE) {
      dataTagAddressUpdate.setValueDeadbandType(dataTagAddress.getValueDeadbandType());
      dataTagAddressUpdate.setValueDeadband(dataTagAddress.getValueDeadband());
    } else {
      dataTagAddressUpdate.addFieldToRemove("valueDeadbandType");
      dataTagAddressUpdate.addFieldToRemove("valueDeadband");
    }
    if (dataTagAddress.getTimeDeadband() != DataTagDeadband.DEADBAND_NONE) {
      dataTagAddressUpdate.setTimeDeadband(dataTagAddress.getTimeDeadband());
    } else {
      dataTagAddressUpdate.addFieldToRemove("timeDeadband");
    }
    if (dataTagAddress.getHardwareAddress() != null) {
      HardwareAddressUpdate hardwareAddressUpdate = new HardwareAddressUpdate(dataTagAddress.getHardwareAddress().getClass().getName());
      dataTagAddressUpdate.setHardwareAddressUpdate(hardwareAddressUpdate);
      SimpleTypeReflectionHandler reflectionHandler = new SimpleTypeReflectionHandler();
      for (Field field : reflectionHandler.getNonTransientSimpleFields(dataTagAddress.getHardwareAddress().getClass())) {
        field.setAccessible(true);
        hardwareAddressUpdate.getChangedValues().put(field.getName(), field.get(dataTagAddress.getHardwareAddress()));
      }
    }
    if (dataTagAddress.getFreshnessInterval() != null) {
      dataTagAddressUpdate.setFreshnessInterval(dataTagAddress.getFreshnessInterval());
    }
  }

  /**
   * Checks that the AbstractTagCacheObject passes all validation tests for
   * being included in TIM. This method should be called during runtime
   * reconfigurations for instance.
   *
   * @param tag the tag to validate
   * @throws ConfigurationException if a validation test fails
   */
  protected void validateTagConfig(final T tag) throws ConfigurationException {

    new MicroValidator<>(tag)
      .notNull(Tag::getId, "id")
      .notNull(Tag::getName, "name")
      .not(tagObj -> tagObj.getName().isEmpty(), "Parameter \"name\" cannot be empty") // This had a commented out max check as well, do we want that?
      .notNull(Tag::getDataType, "dataType")
      .between(Tag::getMode, MODE_OPERATIONAL, MODE_TEST, "mode")
      .not(tagObj -> tagObj.getUnit() != null && tag.getUnit().length() > 20, "Parameter \"unit\" must be 0 to 20 characters long");
  }
}
