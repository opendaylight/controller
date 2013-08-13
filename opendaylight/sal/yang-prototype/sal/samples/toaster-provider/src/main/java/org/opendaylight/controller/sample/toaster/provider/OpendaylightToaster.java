package org.opendaylight.controller.sample.toaster.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.Futures;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToastDone;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToastDone.ToastStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToastDoneBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToasterBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToasterData;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToasterService;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpendaylightToaster implements ToasterData, ToasterService {

	private static final Logger log = LoggerFactory.getLogger(OpendaylightToaster.class);
	
	private static final DisplayString toasterManufacturer = new DisplayString("Opendaylight");
	private static final DisplayString toasterModelNumber = new DisplayString("Model 1 - Binding Aware");
	private ToasterStatus toasterStatus;
	
	private NotificationProviderService notificationProvider;
	private final ExecutorService executor;
	
	private Future<RpcResult<Void>> currentTask;
	
	public OpendaylightToaster() {
		toasterStatus = ToasterStatus.Down;
		executor = Executors.newFixedThreadPool(1);
	}
	
	
	@Override
	public Toaster getToaster() {
		ToasterBuilder tb = new ToasterBuilder();
		tb //
		 .setToasterManufacturer(toasterManufacturer) //
		 .setToasterModelNumber(toasterModelNumber) //
		 .setToasterStatus(toasterStatus);
		
		return tb.build();
	}


	@Override
	public Future<RpcResult<Void>> cancelToast() {
		if(currentTask != null) {
			cancelToastImpl();
		}
		return null;
	}

	@Override
	public Future<RpcResult<Void>> makeToast(MakeToastInput input) {
		// TODO Auto-generated method stub
		log.info("makeToast - Received input for toast");
		logToastInput(input);
		if(currentTask != null) {
			return inProgressError();
		}
		currentTask = executor.submit(new MakeToastTask(input));
		return currentTask;
	}
	
	
	private Future<RpcResult<Void>> inProgressError() {
		RpcResult<Void> result = Rpcs.<Void>getRpcResult(false, null, Collections.<RpcError>emptySet());
		return Futures.immediateFuture(result);
	}


	private void cancelToastImpl() {
		currentTask.cancel(true);
		ToastDoneBuilder toastDone = new ToastDoneBuilder();
		toastDone.setToastStatus(ToastStatus.Cancelled);
		notificationProvider.notify(toastDone.build());
	}


	public void setNotificationProvider(NotificationProviderService salService) {
		this.notificationProvider = salService;
	}
	
	private void logToastInput(MakeToastInput input) {
		String toastType = input.getToasterToastType().getName();
		String toastDoneness = input.getToasterDoneness().toString();
		log.info("Toast: " + toastType + " doneness: " + toastDoneness);
	}
	

	private class MakeToastTask implements Callable<RpcResult<Void>> {
		
		final MakeToastInput toastRequest;
		
		public MakeToastTask(MakeToastInput toast) {
			toastRequest = toast;
		}
		
		@Override
		public RpcResult<Void> call() throws Exception {
			Thread.sleep(1000);
			
			ToastDoneBuilder notifyBuilder = new ToastDoneBuilder();
			notifyBuilder.setToastStatus(ToastStatus.Done);
			notificationProvider.notify(notifyBuilder.build());
			log.info("Toast Done");
			return Rpcs.<Void>getRpcResult(true, null, Collections.<RpcError>emptySet());
		}		
	}
}
