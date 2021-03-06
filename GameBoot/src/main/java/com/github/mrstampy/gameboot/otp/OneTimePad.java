/*
 *              ______                        ____              __ 
 *             / ____/___ _____ ___  ___     / __ )____  ____  / /_
 *            / / __/ __ `/ __ `__ \/ _ \   / __  / __ \/ __ \/ __/
 *           / /_/ / /_/ / / / / / /  __/  / /_/ / /_/ / /_/ / /_  
 *           \____/\__,_/_/ /_/ /_/\___/  /_____/\____/\____/\__/  
 *                                                 
 *                                 .-'\
 *                              .-'  `/\
 *                           .-'      `/\
 *                           \         `/\
 *                            \         `/\
 *                             \    _-   `/\       _.--.
 *                              \    _-   `/`-..--\     )
 *                               \    _-   `,','  /    ,')
 *                                `-_   -   ` -- ~   ,','
 *                                 `-              ,','
 *                                  \,--.    ____==-~
 *                                   \   \_-~\
 *                                    `_-~_.-'
 *                                     \-~
 * 
 *                       http://mrstampy.github.io/gameboot/
 *
 * Copyright (C) 2015, 2016 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.gameboot.otp;

import java.security.SecureRandom;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.security.SecurityConfiguration;

/**
 * The Class OneTimePad is an implementation of the
 * <a href="https://en.wikipedia.org/wiki/One-time_pad">One Time Pad</a>
 * algorithm. The intended use is to generate a secret key which is passed
 * securely to the client (https, wss etc). All messages are then encrypted and
 * decrypted with this key and the encrypted messages can be exchanged clear
 * text (http, ws etc), negating the overhead of secure connections.<br>
 * <br>
 * 
 * Key length must be greater or equal to the message length and a power of 2.
 * 
 * @see KeyRegistry
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OneTimePad {

  /** The Constant OTP_KEY_GENERATION. */
  public static final String OTP_KEY_GENERATION = "OTP key generation timer";

  /** The Constant OTP_CONVERSION. */
  public static final String OTP_CONVERSION = "OTP message conversion timer";

  @Autowired
  @Qualifier(SecurityConfiguration.GAME_BOOT_SECURE_RANDOM)
  private SecureRandom random;

  @Autowired
  private MetricsHelper helper;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.timer(OTP_KEY_GENERATION, getClass(), "otp", "key", "generation", "timer");
    helper.timer(OTP_CONVERSION, getClass(), "otp", "message", "conversion", "timer");
  }

  /**
   * Generate key to share securely (https/wss) with the client.
   *
   * @param size
   *          the size of the key string
   * @return the string
   * @throws Exception
   *           the exception
   * @see KeyRegistry
   */
  public byte[] generateKey(int size) throws Exception {
    Optional<Context> ctx = helper.startTimer(OTP_KEY_GENERATION);
    try {
      check(size);

      byte[] key = new byte[size];

      random.nextBytes(key);

      return key;
    } finally {
      helper.stopTimer(ctx);
    }
  }

  /**
   * Will encode the message if decoded, decode the message if encoded.
   *
   * @param key
   *          the key
   * @param message
   *          the message byte array
   * @return the converted byte array
   * @throws Exception
   *           the exception
   */
  public byte[] convert(byte[] key, byte[] message) throws Exception {
    Optional<Context> ctx = helper.startTimer(OTP_CONVERSION);
    try {
      check(key, message);

      byte[] converted = new byte[message.length];

      for (int i = 0; i < message.length; i++) {
        converted[i] = (byte) (message[i] ^ key[i]);
      }

      return converted;
    } finally {
      helper.stopTimer(ctx);
    }
  }

  private void check(int size) {
    if (size <= 0) fail("Size must be > 0");
  }

  private void check(byte[] key, byte[] message) {
    keyCheck(key);
    if (mtArray(message)) fail("No message");

    lengthCheck(key, message);
  }

  private void lengthCheck(byte[] key, byte[] message) {
    if (key.length < message.length) fail("Key length too short for message");
  }

  private void keyCheck(byte[] key) {
    if (mtArray(key)) fail("No key");
  }

  private boolean mtArray(byte[] key) {
    return key == null || key.length == 0;
  }

  private void fail(String message) {
    throw new IllegalArgumentException(message);
  }
}
