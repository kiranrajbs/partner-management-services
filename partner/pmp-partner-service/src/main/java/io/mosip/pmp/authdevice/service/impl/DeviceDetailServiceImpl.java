package io.mosip.pmp.authdevice.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.core.util.EmptyCheckUtils;
import io.mosip.pmp.authdevice.constants.DeviceDetailExceptionsConstant;
import io.mosip.pmp.authdevice.dto.DeviceDetailDto;
import io.mosip.pmp.authdevice.dto.DeviceDetailUpdateDto;
import io.mosip.pmp.authdevice.dto.IdDto;
import io.mosip.pmp.authdevice.dto.UpdateDeviceDetailStatusDto;
import io.mosip.pmp.authdevice.entity.DeviceDetail;
import io.mosip.pmp.authdevice.entity.RegistrationDeviceSubType;
import io.mosip.pmp.authdevice.exception.RequestException;
import io.mosip.pmp.authdevice.repository.DeviceDetailRepository;
import io.mosip.pmp.authdevice.repository.RegistrationDeviceSubTypeRepository;
import io.mosip.pmp.authdevice.service.DeviceDetailService;
import io.mosip.pmp.authdevice.util.AuditUtil;
import io.mosip.pmp.authdevice.util.AuthDeviceConstant;
import io.mosip.pmp.partner.repository.PartnerServiceRepository;


@Component
@Transactional
public class DeviceDetailServiceImpl implements DeviceDetailService {

	private static final String PENDING_APPROVAL = "Pending_Approval";
	
	@Autowired
	AuditUtil auditUtil;
	
	@Autowired
	DeviceDetailRepository deviceDetailRepository;

	@Autowired
	RegistrationDeviceSubTypeRepository registrationDeviceSubTypeRepository;

	@Autowired
	PartnerServiceRepository partnerRepository;
	
	@Override
	public IdDto createDeviceDetails(DeviceDetailDto deviceDetailDto) {
		DeviceDetail entity=new DeviceDetail();
		DeviceDetail deviceDetail=null;
		IdDto dto=new IdDto();
		
		RegistrationDeviceSubType registrationDeviceSubType=registrationDeviceSubTypeRepository
				.findByCodeAndTypeCodeAndIsDeletedFalseorIsDeletedIsNullAndIsActiveTrue(deviceDetailDto.getDeviceSubTypeCode(),deviceDetailDto.getDeviceTypeCode());
		if (registrationDeviceSubType == null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_CREATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorCode(),
							DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorMessage()),
					"AUT-002");
			throw new RequestException(DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorCode(),
					DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorMessage());
		}else {
			entity.setDeviceSubTypeCode(registrationDeviceSubType.getCode());
			entity.setDeviceTypeCode(registrationDeviceSubType.getDeviceTypeCode());
		}
		if ((partnerRepository
				.findByIdAndIsDeletedFalseorIsDeletedIsNullAndIsActiveTrue(deviceDetailDto.getDeviceProviderId())) == null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_CREATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorCode(),
							DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorMessage()),
					"AUT-003");
			throw new RequestException(DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorCode(),
					DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorMessage());
		}

		if (deviceDetailRepository.findByDeviceDetail( deviceDetailDto.getMake(),
				deviceDetailDto.getModel(),  deviceDetailDto.getDeviceProviderId(),
				deviceDetailDto.getDeviceSubTypeCode(),deviceDetailDto.getDeviceTypeCode()) != null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_CREATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.DEVICE_DETAIL_EXIST.getErrorCode(),
							DeviceDetailExceptionsConstant.DEVICE_DETAIL_EXIST.getErrorMessage()),
					"AUT-004");
			throw new RequestException(DeviceDetailExceptionsConstant.DEVICE_DETAIL_EXIST.getErrorCode(),
					DeviceDetailExceptionsConstant.DEVICE_DETAIL_EXIST.getErrorMessage());
		}
		entity=getCreateMapping(entity,deviceDetailDto);
		deviceDetail=deviceDetailRepository.save(entity);
		dto.setId(deviceDetail.getId());
	return dto;
	}
	
	private DeviceDetail getCreateMapping(DeviceDetail deviceDetail,DeviceDetailDto deviceDetailDto) {
		deviceDetail.setId(deviceDetailDto.getId());
		deviceDetail.setIsActive(false);
		deviceDetail.setApprovalStatus(PENDING_APPROVAL);
		Authentication authN = SecurityContextHolder.getContext().getAuthentication();
		if (!EmptyCheckUtils.isNullEmpty(authN)) {
			deviceDetail.setCrBy(authN.getName());
		}
		deviceDetail.setCrDtimes(LocalDateTime.now(ZoneId.of("UTC")));
		deviceDetail.setDeviceProviderId(deviceDetailDto.getDeviceProviderId());
		deviceDetail.setMake(deviceDetailDto.getMake());
		deviceDetail.setModel(deviceDetailDto.getModel());
		deviceDetail.setPartnerOrganizationName(deviceDetailDto.getPartnerOrganizationName());
		return deviceDetail;
		
	}

	@Override
	public IdDto updateDeviceDetails(DeviceDetailUpdateDto deviceDetailDto) {
		DeviceDetail entity=new DeviceDetail();
		DeviceDetail deviceDetail=null;
		IdDto dto=new IdDto();
		entity=deviceDetailRepository.findByIdAndIsDeletedFalseOrIsDeletedIsNull(deviceDetailDto.getId());
		if (entity == null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_UPDATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorCode(),
							DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorMessage()),
					"AUT-008");
			throw new RequestException(DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorCode(),
					String.format(DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorMessage(), dto.getId()));
		}
		RegistrationDeviceSubType registrationDeviceSubType=registrationDeviceSubTypeRepository
				.findByCodeAndTypeCodeAndIsDeletedFalseorIsDeletedIsNullAndIsActiveTrue(deviceDetailDto.getDeviceSubTypeCode(),deviceDetailDto.getDeviceTypeCode());
		if (registrationDeviceSubType == null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_UPDATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorCode(),
							DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorMessage()),
					"AUT-009");
			throw new RequestException(DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorCode(),
					DeviceDetailExceptionsConstant.REG_DEVICE_SUB_TYPE_NOT_FOUND.getErrorMessage());
		}else {
			entity.setDeviceSubTypeCode(registrationDeviceSubType.getCode());
			entity.setDeviceTypeCode(registrationDeviceSubType.getDeviceTypeCode());
		}
		if ((partnerRepository
			.findByIdAndIsDeletedFalseorIsDeletedIsNullAndIsActiveTrue(deviceDetailDto.getDeviceProviderId())) == null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_UPDATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorCode(),
							DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorMessage()),
					"AUT-010");
			throw new RequestException(DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorCode(),
					DeviceDetailExceptionsConstant.DEVICE_PROVIDER_NOT_FOUND.getErrorMessage());
		}
		
		entity=getUpdateMapping(entity,deviceDetailDto);
		deviceDetail=deviceDetailRepository.save(entity);
		dto.setId(deviceDetail.getId());
		return dto;
	}
	
	private DeviceDetail getUpdateMapping(DeviceDetail deviceDetail,DeviceDetailUpdateDto deviceDetailDto) {
		deviceDetail.setId(deviceDetailDto.getId());
		deviceDetail.setIsActive(deviceDetailDto.getIsActive());
		
		Authentication authN = SecurityContextHolder.getContext().getAuthentication();
		if (!EmptyCheckUtils.isNullEmpty(authN)) {
			deviceDetail.setUpdBy(authN.getName());
		}
		deviceDetail.setUpdDtimes(LocalDateTime.now(ZoneId.of("UTC")));
		deviceDetail.setDeviceProviderId(deviceDetailDto.getDeviceProviderId());
		deviceDetail.setMake(deviceDetailDto.getMake());
		deviceDetail.setModel(deviceDetailDto.getModel());
		deviceDetail.setPartnerOrganizationName(deviceDetailDto.getPartnerOrganizationName());
		return deviceDetail;
		
	}
	
	@Override
	public String updateDeviceDetailStatus(UpdateDeviceDetailStatusDto deviceDetails) {
		DeviceDetail entity=deviceDetailRepository.findByIdAndIsDeletedFalseOrIsDeletedIsNull(deviceDetails.getId());
		if (entity == null) {
			auditUtil.auditRequest(
					String.format(
							AuthDeviceConstant.FAILURE_UPDATE, DeviceDetail.class.getCanonicalName()),
					AuthDeviceConstant.AUDIT_SYSTEM,
					String.format(AuthDeviceConstant.FAILURE_DESC,
							DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorCode(),
							DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorMessage()),
					"AUT-008");
			throw new RequestException(DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorCode(),
					String.format(DeviceDetailExceptionsConstant.DEVICE_DETAIL_NOT_FOUND.getErrorMessage(), deviceDetails.getId()));
		}
		Authentication authN = SecurityContextHolder.getContext().getAuthentication();
		if (!EmptyCheckUtils.isNullEmpty(authN)) {
			entity.setUpdBy(authN.getName());
			entity.setUpdDtimes(LocalDateTime.now(ZoneId.of("UTC")));			
		}
		
		if(deviceDetails.getApprovalStatus().equals(AuthDeviceConstant.APPROVE)) {
			entity.setApprovalStatus(AuthDeviceConstant.APPROVED);	
			entity.setIsActive(true);
			deviceDetailRepository.save(entity);
			return "Device details approved successfully.";
		}
		if(deviceDetails.getApprovalStatus().equals(AuthDeviceConstant.REJECT)) {
			entity.setApprovalStatus(AuthDeviceConstant.REJECTED);	
			entity.setIsActive(false);
			deviceDetailRepository.save(entity);
			return "Device details rejected successfully.";
		}
		
		auditUtil.auditRequest(
				String.format(
						AuthDeviceConstant.STATUS_UPDATE_FAILURE, DeviceDetail.class.getCanonicalName()),
				AuthDeviceConstant.AUDIT_SYSTEM,
				String.format(AuthDeviceConstant.FAILURE_DESC,
						DeviceDetailExceptionsConstant.DEVICE_STATUS_CODE.getErrorCode(),
						DeviceDetailExceptionsConstant.DEVICE_STATUS_CODE.getErrorMessage()),
				"AUT-008");
		throw new RequestException(DeviceDetailExceptionsConstant.DEVICE_STATUS_CODE.getErrorCode(),
				String.format(DeviceDetailExceptionsConstant.DEVICE_STATUS_CODE.getErrorMessage(), deviceDetails.getId()));
	}
}
