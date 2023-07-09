/*
 * Copyright 2006-2008 the V8 project authors. All rights reserved.
 * Java Port Copyright 2021 sir-maniac. All Rights reserved.
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
package cc.tweaked.cobalt.internal.doubles;

import org.checkerframework.checker.signedness.qual.Unsigned;

class PowersOfTenCache {
	/**
	 * Not all powers of ten are cached. The decimal exponent of two neighboring
	 * cached numbers will differ by DECIMAL_EXPONENT_DISTANCE.
	 */
	private static final int DECIMAL_EXPONENT_DISTANCE = 8;

	/**
	 * -1 * the first decimal_exponent.
	 */
	private static final int CACHED_POWERS_OFFSET = 348;
	/**
	 * 1 / lg(10)
	 */
	private static final double D_1_LOG2_10 = 0.30102999566398114;

	/**
	 * Returns a cached power-of-ten with a binary exponent in the range
	 * [minExponent; maxExponent] (boundaries included).
	 */
	public static void getCachedPowerForBinaryExponentRange(int minExponent, int maxExponent, DiyFp[] power, int[] decimalExponent) {
		int kQ = DiyFp.SIGNIFICAND_SIZE;
		double k = Math.ceil((double) (minExponent + kQ - 1) * D_1_LOG2_10);
		int index = (CACHED_POWERS_OFFSET + ((int) k) - 1) / DECIMAL_EXPONENT_DISTANCE + 1;
		assert 0 <= index && index < CACHED_POWERS.length;
		CachedPower cachedPower = CACHED_POWERS[index];
		assert minExponent <= cachedPower.binaryExponent;
		//maxExponent;  // Mark variable as used.
		assert cachedPower.binaryExponent <= maxExponent;
		decimalExponent[0] = cachedPower.decimalExponent;
		power[0] = new DiyFp(cachedPower.significand, cachedPower.binaryExponent);
	}

	private record CachedPower(@Unsigned long significand, int binaryExponent, int decimalExponent) {
	}

	private static final CachedPower[] CACHED_POWERS = {
		new CachedPower(0xfa8f_d5a0_081c_0288L, -1220, -348),
		new CachedPower(0xbaae_e17f_a23e_bf76L, -1193, -340),
		new CachedPower(0x8b16_fb20_3055_ac76L, -1166, -332),
		new CachedPower(0xcf42_894a_5dce_35eaL, -1140, -324),
		new CachedPower(0x9a6b_b0aa_5565_3b2dL, -1113, -316),
		new CachedPower(0xe61a_cf03_3d1a_45dfL, -1087, -308),
		new CachedPower(0xab70_fe17_c79a_c6caL, -1060, -300),
		new CachedPower(0xff77_b1fc_bebc_dc4fL, -1034, -292),
		new CachedPower(0xbe56_91ef_416b_d60cL, -1007, -284),
		new CachedPower(0x8dd0_1fad_907f_fc3cL, -980, -276),
		new CachedPower(0xd351_5c28_3155_9a83L, -954, -268),
		new CachedPower(0x9d71_ac8f_ada6_c9b5L, -927, -260),
		new CachedPower(0xea9c_2277_23ee_8bcbL, -901, -252),
		new CachedPower(0xaecc_4991_4078_536dL, -874, -244),
		new CachedPower(0x823c_1279_5db6_ce57L, -847, -236),
		new CachedPower(0xc210_9436_4dfb_5637L, -821, -228),
		new CachedPower(0x9096_ea6f_3848_984fL, -794, -220),
		new CachedPower(0xd774_85cb_2582_3ac7L, -768, -212),
		new CachedPower(0xa086_cfcd_97bf_97f4L, -741, -204),
		new CachedPower(0xef34_0a98_172a_ace5L, -715, -196),
		new CachedPower(0xb238_67fb_2a35_b28eL, -688, -188),
		new CachedPower(0x84c8_d4df_d2c6_3f3bL, -661, -180),
		new CachedPower(0xc5dd_4427_1ad3_cdbaL, -635, -172),
		new CachedPower(0x936b_9fce_bb25_c996L, -608, -164),
		new CachedPower(0xdbac_6c24_7d62_a584L, -582, -156),
		new CachedPower(0xa3ab_6658_0d5f_daf6L, -555, -148),
		new CachedPower(0xf3e2_f893_dec3_f126L, -529, -140),
		new CachedPower(0xb5b5_ada8_aaff_80b8L, -502, -132),
		new CachedPower(0x8762_5f05_6c7c_4a8bL, -475, -124),
		new CachedPower(0xc9bc_ff60_34c1_3053L, -449, -116),
		new CachedPower(0x964e_858c_91ba_2655L, -422, -108),
		new CachedPower(0xdff9_7724_7029_7ebdL, -396, -100),
		new CachedPower(0xa6df_bd9f_b8e5_b88fL, -369, -92),
		new CachedPower(0xf8a9_5fcf_8874_7d94L, -343, -84),
		new CachedPower(0xb944_7093_8fa8_9bcfL, -316, -76),
		new CachedPower(0x8a08_f0f8_bf0f_156bL, -289, -68),
		new CachedPower(0xcdb0_2555_6531_31b6L, -263, -60),
		new CachedPower(0x993f_e2c6_d07b_7facL, -236, -52),
		new CachedPower(0xe45c_10c4_2a2b_3b06L, -210, -44),
		new CachedPower(0xaa24_2499_6973_92d3L, -183, -36),
		new CachedPower(0xfd87_b5f2_8300_ca0eL, -157, -28),
		new CachedPower(0xbce5_0864_9211_1aebL, -130, -20),
		new CachedPower(0x8cbc_cc09_6f50_88ccL, -103, -12),
		new CachedPower(0xd1b7_1758_e219_652cL, -77, -4),
		new CachedPower(0x9c40_0000_0000_0000L, -50, 4),
		new CachedPower(0xe8d4_a510_0000_0000L, -24, 12),
		new CachedPower(0xad78_ebc5_ac62_0000L, 3, 20),
		new CachedPower(0x813f_3978_f894_0984L, 30, 28),
		new CachedPower(0xc097_ce7b_c907_15b3L, 56, 36),
		new CachedPower(0x8f7e_32ce_7bea_5c70L, 83, 44),
		new CachedPower(0xd5d2_38a4_abe9_8068L, 109, 52),
		new CachedPower(0x9f4f_2726_179a_2245L, 136, 60),
		new CachedPower(0xed63_a231_d4c4_fb27L, 162, 68),
		new CachedPower(0xb0de_6538_8cc8_ada8L, 189, 76),
		new CachedPower(0x83c7_088e_1aab_65dbL, 216, 84),
		new CachedPower(0xc45d_1df9_4271_1d9aL, 242, 92),
		new CachedPower(0x924d_692c_a61b_e758L, 269, 100),
		new CachedPower(0xda01_ee64_1a70_8deaL, 295, 108),
		new CachedPower(0xa26d_a399_9aef_774aL, 322, 116),
		new CachedPower(0xf209_787b_b47d_6b85L, 348, 124),
		new CachedPower(0xb454_e4a1_79dd_1877L, 375, 132),
		new CachedPower(0x865b_8692_5b9b_c5c2L, 402, 140),
		new CachedPower(0xc835_53c5_c896_5d3dL, 428, 148),
		new CachedPower(0x952a_b45c_fa97_a0b3L, 455, 156),
		new CachedPower(0xde46_9fbd_99a0_5fe3L, 481, 164),
		new CachedPower(0xa59b_c234_db39_8c25L, 508, 172),
		new CachedPower(0xf6c6_9a72_a398_9f5cL, 534, 180),
		new CachedPower(0xb7dc_bf53_54e9_beceL, 561, 188),
		new CachedPower(0x88fc_f317_f222_41e2L, 588, 196),
		new CachedPower(0xcc20_ce9b_d35c_78a5L, 614, 204),
		new CachedPower(0x9816_5af3_7b21_53dfL, 641, 212),
		new CachedPower(0xe2a0_b5dc_971f_303aL, 667, 220),
		new CachedPower(0xa8d9_d153_5ce3_b396L, 694, 228),
		new CachedPower(0xfb9b_7cd9_a4a7_443cL, 720, 236),
		new CachedPower(0xbb76_4c4c_a7a4_4410L, 747, 244),
		new CachedPower(0x8bab_8eef_b640_9c1aL, 774, 252),
		new CachedPower(0xd01f_ef10_a657_842cL, 800, 260),
		new CachedPower(0x9b10_a4e5_e991_3129L, 827, 268),
		new CachedPower(0xe710_9bfb_a19c_0c9dL, 853, 276),
		new CachedPower(0xac28_20d9_623b_f429L, 880, 284),
		new CachedPower(0x8044_4b5e_7aa7_cf85L, 907, 292),
		new CachedPower(0xbf21_e440_03ac_dd2dL, 933, 300),
		new CachedPower(0x8e67_9c2f_5e44_ff8fL, 960, 308),
		new CachedPower(0xd433_179d_9c8c_b841L, 986, 316),
		new CachedPower(0x9e19_db92_b4e3_1ba9L, 1013, 324),
		new CachedPower(0xeb96_bf6e_badf_77d9L, 1039, 332),
		new CachedPower(0xaf87_023b_9bf0_ee6bL, 1066, 340)
	};

}
