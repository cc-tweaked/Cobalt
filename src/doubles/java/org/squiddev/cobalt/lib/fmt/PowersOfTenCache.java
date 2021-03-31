/*
 * Copyright 2006-2008 the V8 project authors. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Google Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.squiddev.cobalt.lib.fmt;

import org.checkerframework.checker.signedness.qual.Unsigned;

import static org.squiddev.cobalt.lib.fmt.Assert.DOUBLE_CONVERSION_ASSERT;

public class PowersOfTenCache {
	/**
	 *  Not all powers of ten are cached. The decimal exponent of two neighboring
	 *  cached numbers will differ by kDecimalExponentDistance.
	 */
	public static final int kDecimalExponentDistance = 8;

	public static final int kMinDecimalExponent = -348;
	public static final int kMaxDecimalExponent = 340;

	static class CachedPower {
		@Unsigned long significand;
		short binary_exponent;
		short decimal_exponent;

		CachedPower(@Unsigned long significand, short binaryExponent, short decimalExponent) {
			this.significand = significand;
			this.binary_exponent = binaryExponent;
			this.decimal_exponent = decimalExponent;
		}
	}

	private static final CachedPower[] kCachedPowers = {
			new CachedPower(0xfa8f_d5a0_081c_0288L, (short)-1220, (short)-348),
			new CachedPower(0xbaae_e17f_a23e_bf76L, (short)-1193, (short)-340),
			new CachedPower(0x8b16_fb20_3055_ac76L, (short)-1166, (short)-332),
			new CachedPower(0xcf42_894a_5dce_35eaL, (short)-1140, (short)-324),
			new CachedPower(0x9a6b_b0aa_5565_3b2dL, (short)-1113, (short)-316),
			new CachedPower(0xe61a_cf03_3d1a_45dfL, (short)-1087, (short)-308),
			new CachedPower(0xab70_fe17_c79a_c6caL, (short)-1060, (short)-300),
			new CachedPower(0xff77_b1fc_bebc_dc4fL, (short)-1034, (short)-292),
			new CachedPower(0xbe56_91ef_416b_d60cL, (short)-1007, (short)-284),
			new CachedPower(0x8dd0_1fad_907f_fc3cL, (short)-980, (short)-276),
			new CachedPower(0xd351_5c28_3155_9a83L, (short)-954, (short)-268),
			new CachedPower(0x9d71_ac8f_ada6_c9b5L, (short)-927, (short)-260),
			new CachedPower(0xea9c_2277_23ee_8bcbL, (short)-901, (short)-252),
			new CachedPower(0xaecc_4991_4078_536dL, (short)-874, (short)-244),
			new CachedPower(0x823c_1279_5db6_ce57L, (short)-847, (short)-236),
			new CachedPower(0xc210_9436_4dfb_5637L, (short)-821, (short)-228),
			new CachedPower(0x9096_ea6f_3848_984fL, (short)-794, (short)-220),
			new CachedPower(0xd774_85cb_2582_3ac7L, (short)-768, (short)-212),
			new CachedPower(0xa086_cfcd_97bf_97f4L, (short)-741, (short)-204),
			new CachedPower(0xef34_0a98_172a_ace5L, (short)-715, (short)-196),
			new CachedPower(0xb238_67fb_2a35_b28eL, (short)-688, (short)-188),
			new CachedPower(0x84c8_d4df_d2c6_3f3bL, (short)-661, (short)-180),
			new CachedPower(0xc5dd_4427_1ad3_cdbaL, (short)-635, (short)-172),
			new CachedPower(0x936b_9fce_bb25_c996L, (short)-608, (short)-164),
			new CachedPower(0xdbac_6c24_7d62_a584L, (short)-582, (short)-156),
			new CachedPower(0xa3ab_6658_0d5f_daf6L, (short)-555, (short)-148),
			new CachedPower(0xf3e2_f893_dec3_f126L, (short)-529, (short)-140),
			new CachedPower(0xb5b5_ada8_aaff_80b8L, (short)-502, (short)-132),
			new CachedPower(0x8762_5f05_6c7c_4a8bL, (short)-475, (short)-124),
			new CachedPower(0xc9bc_ff60_34c1_3053L, (short)-449, (short)-116),
			new CachedPower(0x964e_858c_91ba_2655L, (short)-422, (short)-108),
			new CachedPower(0xdff9_7724_7029_7ebdL, (short)-396, (short)-100),
			new CachedPower(0xa6df_bd9f_b8e5_b88fL, (short)-369, (short)-92),
			new CachedPower(0xf8a9_5fcf_8874_7d94L, (short)-343, (short)-84),
			new CachedPower(0xb944_7093_8fa8_9bcfL, (short)-316, (short)-76),
			new CachedPower(0x8a08_f0f8_bf0f_156bL, (short)-289, (short)-68),
			new CachedPower(0xcdb0_2555_6531_31b6L, (short)-263, (short)-60),
			new CachedPower(0x993f_e2c6_d07b_7facL, (short)-236, (short)-52),
			new CachedPower(0xe45c_10c4_2a2b_3b06L, (short)-210, (short)-44),
			new CachedPower(0xaa24_2499_6973_92d3L, (short)-183, (short)-36),
			new CachedPower(0xfd87_b5f2_8300_ca0eL, (short)-157, (short)-28),
			new CachedPower(0xbce5_0864_9211_1aebL, (short)-130, (short)-20),
			new CachedPower(0x8cbc_cc09_6f50_88ccL, (short)-103, (short)-12),
			new CachedPower(0xd1b7_1758_e219_652cL, (short)-77, (short)-4),
			new CachedPower(0x9c40_0000_0000_0000L, (short)-50, (short)4),
			new CachedPower(0xe8d4_a510_0000_0000L, (short)-24, (short)12),
			new CachedPower(0xad78_ebc5_ac62_0000L, (short)3, (short)20),
			new CachedPower(0x813f_3978_f894_0984L, (short)30, (short)28),
			new CachedPower(0xc097_ce7b_c907_15b3L, (short)56, (short)36),
			new CachedPower(0x8f7e_32ce_7bea_5c70L, (short)83, (short)44),
			new CachedPower(0xd5d2_38a4_abe9_8068L, (short)109, (short)52),
			new CachedPower(0x9f4f_2726_179a_2245L, (short)136, (short)60),
			new CachedPower(0xed63_a231_d4c4_fb27L, (short)162, (short)68),
			new CachedPower(0xb0de_6538_8cc8_ada8L, (short)189, (short)76),
			new CachedPower(0x83c7_088e_1aab_65dbL, (short)216, (short)84),
			new CachedPower(0xc45d_1df9_4271_1d9aL, (short)242, (short)92),
			new CachedPower(0x924d_692c_a61b_e758L, (short)269, (short)100),
			new CachedPower(0xda01_ee64_1a70_8deaL, (short)295, (short)108),
			new CachedPower(0xa26d_a399_9aef_774aL, (short)322, (short)116),
			new CachedPower(0xf209_787b_b47d_6b85L, (short)348, (short)124),
			new CachedPower(0xb454_e4a1_79dd_1877L, (short)375, (short)132),
			new CachedPower(0x865b_8692_5b9b_c5c2L, (short)402, (short)140),
			new CachedPower(0xc835_53c5_c896_5d3dL, (short)428, (short)148),
			new CachedPower(0x952a_b45c_fa97_a0b3L, (short)455, (short)156),
			new CachedPower(0xde46_9fbd_99a0_5fe3L, (short)481, (short)164),
			new CachedPower(0xa59b_c234_db39_8c25L, (short)508, (short)172),
			new CachedPower(0xf6c6_9a72_a398_9f5cL, (short)534, (short)180),
			new CachedPower(0xb7dc_bf53_54e9_beceL, (short)561, (short)188),
			new CachedPower(0x88fc_f317_f222_41e2L, (short)588, (short)196),
			new CachedPower(0xcc20_ce9b_d35c_78a5L, (short)614, (short)204),
			new CachedPower(0x9816_5af3_7b21_53dfL, (short)641, (short)212),
			new CachedPower(0xe2a0_b5dc_971f_303aL, (short)667, (short)220),
			new CachedPower(0xa8d9_d153_5ce3_b396L, (short)694, (short)228),
			new CachedPower(0xfb9b_7cd9_a4a7_443cL, (short)720, (short)236),
			new CachedPower(0xbb76_4c4c_a7a4_4410L, (short)747, (short)244),
			new CachedPower(0x8bab_8eef_b640_9c1aL, (short)774, (short)252),
			new CachedPower(0xd01f_ef10_a657_842cL, (short)800, (short)260),
			new CachedPower(0x9b10_a4e5_e991_3129L, (short)827, (short)268),
			new CachedPower(0xe710_9bfb_a19c_0c9dL, (short)853, (short)276),
			new CachedPower(0xac28_20d9_623b_f429L, (short)880, (short)284),
			new CachedPower(0x8044_4b5e_7aa7_cf85L, (short)907, (short)292),
			new CachedPower(0xbf21_e440_03ac_dd2dL, (short)933, (short)300),
			new CachedPower(0x8e67_9c2f_5e44_ff8fL, (short)960, (short)308),
			new CachedPower(0xd433_179d_9c8c_b841L, (short)986, (short)316),
			new CachedPower(0x9e19_db92_b4e3_1ba9L, (short)1013, (short)324),
			new CachedPower(0xeb96_bf6e_badf_77d9L, (short)1039, (short)332),
			new CachedPower(0xaf87_023b_9bf0_ee6bL, (short)1066, (short)340)
	};

	/** -1 * the first decimal_exponent. */
	private static final int kCachedPowersOffset = 348;
	/** 1 / lg(10) */
	private static final double kD_1_LOG2_10 = 0.30102999566398114;

	/**
	 * Returns a cached power-of-ten with a binary exponent in the range
	 * [min_exponent; max_exponent] (boundaries included).
	 */
	public static void GetCachedPowerForBinaryExponentRange(
			int min_exponent,
			int max_exponent,
			DiyFp[] power,
			int[] decimal_exponent) {
		int kQ = DiyFp.kSignificandSize;
		double k = Math.ceil((min_exponent + kQ - 1) * kD_1_LOG2_10);
		int foo = kCachedPowersOffset;
		int index =
				(foo + ((int)k) - 1) / kDecimalExponentDistance + 1;
		DOUBLE_CONVERSION_ASSERT(0 <= index && index < kCachedPowers.length);
		CachedPower cached_power = kCachedPowers[index];
		DOUBLE_CONVERSION_ASSERT(min_exponent <= cached_power.binary_exponent);
		//max_exponent;  // Mark variable as used.
		DOUBLE_CONVERSION_ASSERT(cached_power.binary_exponent <= max_exponent);
		decimal_exponent[0] = cached_power.decimal_exponent;
		power[0] = new DiyFp(UnsignedLong.uValueOf(cached_power.significand), cached_power.binary_exponent);
	}

	/**
	 *  Returns a cached power of ten x ~= 10^k such that
	 *    k <= decimal_exponent < k + kCachedPowersDecimalDistance.
	 *  The given decimal_exponent must satisfy
	 *    kMinDecimalExponent <= requested_exponent, and
	 *    requested_exponent < kMaxDecimalExponent + kDecimalExponentDistance.
	 */
	void GetCachedPowerForDecimalExponent(int requested_exponent,
										  DiyFp[] power,
										  int[] found_exponent) {
		DOUBLE_CONVERSION_ASSERT(kMinDecimalExponent <= requested_exponent);
		DOUBLE_CONVERSION_ASSERT(requested_exponent < kMaxDecimalExponent + kDecimalExponentDistance);
		int index =
				(requested_exponent + kCachedPowersOffset) / kDecimalExponentDistance;
		CachedPower cached_power = kCachedPowers[index];
		power[0] = new DiyFp(UnsignedLong.uValueOf(cached_power.significand), cached_power.binary_exponent);
		found_exponent[0] = cached_power.decimal_exponent;
		DOUBLE_CONVERSION_ASSERT(found_exponent[0] <= requested_exponent);
		DOUBLE_CONVERSION_ASSERT(requested_exponent < found_exponent[0] + kDecimalExponentDistance);
	}

}
