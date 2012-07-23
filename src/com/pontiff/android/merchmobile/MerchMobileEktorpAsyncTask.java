package com.pontiff.android.merchmobile;

import org.ektorp.DbAccessException;
import org.ektorp.android.util.EktorpAsyncTask;

import android.util.Log;

public abstract class MerchMobileEktorpAsyncTask extends EktorpAsyncTask {

	@Override
	protected void onDbAccessException(DbAccessException dbAccessException) {
		Log.e(MerchMobileActivity.TAG, "DbAccessException in background", dbAccessException);
	}

}
