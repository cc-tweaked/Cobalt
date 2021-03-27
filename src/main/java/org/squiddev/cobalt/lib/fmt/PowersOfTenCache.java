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

public class PowersOfTenCache {
	// Not all powers of ten are cached. The decimal exponent of two neighboring
	// cached numbers will differ by kDecimalExponentDistance.
	public static final int kDecimalExponentDistance = 8;

	public static final int kMinDecimalExponent = -348;
	public static final int kMaxDecimalExponent = 340;

	class CachedPower {
		uint64_t significand;
		int16_t binary_exponent;
		int16_t decimal_exponent;
	};


	private static final CachedPower kCachedPowers[] = {
			{0xfa8fd5a0081c0288L, -1220, -348},
			{0xbaaee17fa23ebf76L, -1193, -340},
			{0x8b16fb203055ac76L, -1166, -332},
			{0xcf42894a5dce35eaL, -1140, -324},
			{0x9a6bb0aa55653b2dL, -1113, -316},
			{0xe61acf033d1a45dfL, -1087, -308},
			{0xab70fe17c79ac6caL, -1060, -300},
			{0xff77b1fcbebcdc4fL, -1034, -292},
			{0xbe5691ef416bd60cL, -1007, -284},
			{0x8dd01fad907ffc3cL, -980, -276},
			{0xd3515c2831559a83L, -954, -268},
			{0x9d71ac8fada6c9b5L, -927, -260},
			{0xea9c227723ee8bcbL, -901, -252},
			{0xaecc49914078536dL, -874, -244},
			{0x823c12795db6ce57L, -847, -236},
			{0xc21094364dfb5637L, -821, -228},
			{0x9096ea6f3848984fL, -794, -220},
			{0xd77485cb25823ac7L, -768, -212},
			{0xa086cfcd97bf97f4L, -741, -204},
			{0xef340a98172aace5L, -715, -196},
			{0xb23867fb2a35b28eL, -688, -188},
			{0x84c8d4dfd2c63f3bL, -661, -180},
			{0xc5dd44271ad3cdbaL, -635, -172},
			{0x936b9fcebb25c996L, -608, -164},
			{0xdbac6c247d62a584L, -582, -156},
			{0xa3ab66580d5fdaf6L, -555, -148},
			{0xf3e2f893dec3f126L, -529, -140},
			{0xb5b5ada8aaff80b8L, -502, -132},
			{0x87625f056c7c4a8bL, -475, -124},
			{0xc9bcff6034c13053L, -449, -116},
			{0x964e858c91ba2655L, -422, -108},
			{0xdff9772470297ebdL, -396, -100},
			{0xa6dfbd9fb8e5b88fL, -369, -92},
			{0xf8a95fcf88747d94L, -343, -84},
			{0xb94470938fa89bcfL, -316, -76},
			{0x8a08f0f8bf0f156bL, -289, -68},
			{0xcdb02555653131b6L, -263, -60},
			{0x993fe2c6d07b7facL, -236, -52},
			{0xe45c10c42a2b3b06L, -210, -44},
			{0xaa242499697392d3L, -183, -36},
			{0xfd87b5f28300ca0eL, -157, -28},
			{0xbce5086492111aebL, -130, -20},
			{0x8cbccc096f5088ccL, -103, -12},
			{0xd1b71758e219652cL, -77, -4},
			{0x9c40000000000000L, -50, 4},
			{0xe8d4a51000000000L, -24, 12},
			{0xad78ebc5ac620000L, 3, 20},
			{0x813f3978f8940984L, 30, 28},
			{0xc097ce7bc90715b3L, 56, 36},
			{0x8f7e32ce7bea5c70L, 83, 44},
			{0xd5d238a4abe98068L, 109, 52},
			{0x9f4f2726179a2245L, 136, 60},
			{0xed63a231d4c4fb27L, 162, 68},
			{0xb0de65388cc8ada8L, 189, 76},
			{0x83c7088e1aab65dbL, 216, 84},
			{0xc45d1df942711d9aL, 242, 92},
			{0x924d692ca61be758L, 269, 100},
			{0xda01ee641a708deaL, 295, 108},
			{0xa26da3999aef774aL, 322, 116},
			{0xf209787bb47d6b85L, 348, 124},
			{0xb454e4a179dd1877L, 375, 132},
			{0x865b86925b9bc5c2L, 402, 140},
			{0xc83553c5c8965d3dL, 428, 148},
			{0x952ab45cfa97a0b3L, 455, 156},
			{0xde469fbd99a05fe3L, 481, 164},
			{0xa59bc234db398c25L, 508, 172},
			{0xf6c69a72a3989f5cL, 534, 180},
			{0xb7dcbf5354e9beceL, 561, 188},
			{0x88fcf317f22241e2L, 588, 196},
			{0xcc20ce9bd35c78a5L, 614, 204},
			{0x98165af37b2153dfL, 641, 212},
			{0xe2a0b5dc971f303aL, 667, 220},
			{0xa8d9d1535ce3b396L, 694, 228},
			{0xfb9b7cd9a4a7443cL, 720, 236},
			{0xbb764c4ca7a44410L, 747, 244},
			{0x8bab8eefb6409c1aL, 774, 252},
			{0xd01fef10a657842cL, 800, 260},
			{0x9b10a4e5e9913129L, 827, 268},
			{0xe7109bfba19c0c9dL, 853, 276},
			{0xac2820d9623bf429L, 880, 284},
			{0x80444b5e7aa7cf85L, 907, 292},
			{0xbf21e44003acdd2dL, 933, 300},
			{0x8e679c2f5e44ff8fL, 960, 308},
			{0xd433179d9c8cb841L, 986, 316},
			{0x9e19db92b4e31ba9L, 1013, 324},
			{0xeb96bf6ebadf77d9L, 1039, 332},
			{0xaf87023b9bf0ee6bL, 1066, 340},
		};

		private static final int kCachedPowersOffset = 348;  // -1 * the first decimal_exponent.
		private static final double kD_1_LOG2_10 = 0.30102999566398114;  //  1 / lg(10)

		// Returns a cached power-of-ten with a binary exponent in the range
		// [min_exponent; max_exponent] (boundaries included).
		public void GetCachedPowerForBinaryExponentRange(
				int min_exponent,
				int max_exponent,
				DiyFp* power,
				int* decimal_exponent) {
			int kQ = DiyFp::kSignificandSize;
			double k = ceil((min_exponent + kQ - 1) * kD_1_LOG2_10);
			int foo = kCachedPowersOffset;
			int index =
			(foo + static_cast<int>(k) - 1) / kDecimalExponentDistance + 1;
			DOUBLE_CONVERSION_ASSERT(0 <= index && index < static_cast<int>(DOUBLE_CONVERSION_ARRAY_SIZE(kCachedPowers)));
			CachedPower cached_power = kCachedPowers[index];
			DOUBLE_CONVERSION_ASSERT(min_exponent <= cached_power.binary_exponent);
			(void) max_exponent;  // Mark variable as used.
			DOUBLE_CONVERSION_ASSERT(cached_power.binary_exponent <= max_exponent);
			*decimal_exponent = cached_power.decimal_exponent;
			*power = DiyFp(cached_power.significand, cached_power.binary_exponent);
		}


		// Returns a cached power of ten x ~= 10^k such that
		//   k <= decimal_exponent < k + kCachedPowersDecimalDistance.
		// The given decimal_exponent must satisfy
		//   kMinDecimalExponent <= requested_exponent, and
		//   requested_exponent < kMaxDecimalExponent + kDecimalExponentDistance.
		public void GetCachedPowerForDecimalExponent(int requested_exponent,
				DiyFp* power,
				int* found_exponent) {
			DOUBLE_CONVERSION_ASSERT(kMinDecimalExponent <= requested_exponent);
			DOUBLE_CONVERSION_ASSERT(requested_exponent < kMaxDecimalExponent + kDecimalExponentDistance);
			int index =
			(requested_exponent + kCachedPowersOffset) / kDecimalExponentDistance;
			CachedPower cached_power = kCachedPowers[index];
			*power = DiyFp(cached_power.significand, cached_power.binary_exponent);
			*found_exponent = cached_power.decimal_exponent;
			DOUBLE_CONVERSION_ASSERT(*found_exponent <= requested_exponent);
			DOUBLE_CONVERSION_ASSERT(requested_exponent < *found_exponent + kDecimalExponentDistance);
		}

}
