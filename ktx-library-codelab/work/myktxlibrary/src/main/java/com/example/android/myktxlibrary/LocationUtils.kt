/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.myktxlibrary

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

fun createLocationRequest() = LocationRequest.create().apply {
    interval = 3000
    fastestInterval = 2000
    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
}

fun Location.asString(format: Int = Location.FORMAT_DEGREES): String {
    val latitude = Location.convert(latitude, format)
    val longitude = Location.convert(longitude, format)
    return "$latitude $longitude"
}

@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitLastLocation():Location? =
    suspendCancellableCoroutine<Location>() { continuation ->
        lastLocation.addOnSuccessListener { location ->
            continuation.resume(location, null)
        }.addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }

@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.locationFlow() = callbackFlow<Location> {
    val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (locationResult != null) {
                offer(locationResult.lastLocation)
            }
        }
    }
    requestLocationUpdates(
        createLocationRequest(),
        locationCallback,
        Looper.getMainLooper()
    ).addOnFailureListener { e ->
        close(e)
    }
    awaitClose {
        removeLocationUpdates(locationCallback)
    }
}