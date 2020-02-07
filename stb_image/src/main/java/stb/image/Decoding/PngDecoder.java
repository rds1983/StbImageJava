package stb.image.Decoding;

import stb.image.ColorComponents;
import stb.image.ImageResult;

import java.io.InputStream;

/*public class PngDecoder extends Decoder
{
	private static class stbi__pngchunk
	{
		public long length;
		public long type;
	}

	private static final int STBI__F_none = 0;
	private static final int STBI__F_sub = 1;
	private static final int STBI__F_up = 2;
	private static final int STBI__F_avg = 3;
	private static final int STBI__F_paeth = 4;
	private static final int STBI__F_avg_first = 5;
	private static final int STBI__F_paeth_first = 6;

	private static final short[] first_row_filter =
		{STBI__F_none, STBI__F_sub, STBI__F_none, STBI__F_avg_first, STBI__F_paeth_first};

	private static final short[] stbi__depth_scale_table = { 0, 0xff, 0x55, 0, 0x11, 0, 0, 0, 0x01 };
	private static final short[] png_sig = { 137, 80, 78, 71, 13, 10, 26, 10 };

	protected int img_out_n;

	private int stbi__unpremultiply_on_load;

	private int stbi__de_iphone_flag;

	private short[] idata;
	private short[] expanded;
	private short[] _out_;
	private int depth;

	private PngDecoder(InputStream stream)
	{
		super(stream);
	}

	private stbi__pngchunk stbi__get_chunk_header()
	{
		var c = new stbi__pngchunk();
		c.length = stbi__get32be();
		c.type = stbi__get32be();
		return c;
	}

	private static boolean stbi__check_png_header(InputStream input)
	{
		var i = 0;
		for (i = 0; i < 8; ++i)
			if (input.ReadShort() != png_sig[i])
				return false;

		return true;
	}

	private static int stbi__paeth(int a, int b, int c)
	{
		var p = a + b - c;
		var pa = Math.abs(p - a);
		var pb = Math.abs(p - b);
		var pc = Math.abs(p - c);
		if (pa <= pb && pa <= pc)
			return a;
		if (pb <= pc)
			return b;
		return c;
	}

	private int stbi__create_png_image_raw(FakePtr<Short> raw, long raw_len, int out_n, long x, long y, int depth,
		int color)
	{
		var shorts = depth == 16 ? 2 : 1;
		long i = 0;
		long j = 0;
		var stride = (long)(x * out_n * shorts);
		long img_len = 0;
		long img_width_shorts = 0;
		var k = 0;
		var output_shorts = out_n * shorts;
		var filter_shorts = img_n * shorts;
		var width = (int)x;
		_out_ = new short[x * y * output_shorts];
		img_width_shorts = (long)((img_n * x * depth + 7) >> 3);
		img_len = (img_width_shorts + 1) * y;
		if (raw_len < img_len)
			stbi__err("not enough pixels");
		var ptr = new FakePtr<Short>(_out_);
		for (j = (long)0; j < y; ++j)
		{
			var cur = ptr + stride * j;
			FakePtr<Short> prior;
			var filter = (int)raw.Value;
			raw++;
			if (filter > 4)
				stbi__err("invalid filter");
			if (depth < 8)
			{
				cur += x * out_n - img_width_shorts;
				filter_shorts = 1;
				width = (int)img_width_shorts;
			}

			prior = cur - stride;
			if (j == 0)
				filter = first_row_filter[filter];
			for (k = 0; k < filter_shorts; ++k)
				switch (filter)
				{
					case STBI__F_none:
						cur[k] = raw[k];
						break;
					case STBI__F_sub:
						cur[k] = raw[k];
						break;
					case STBI__F_up:
						cur[k] = (short)((raw[k] + prior[k]) & 255);
						break;
					case STBI__F_avg:
						cur[k] = (short)((raw[k] + (prior[k] >> 1)) & 255);
						break;
					case STBI__F_paeth:
						cur[k] = (short)((raw[k] + stbi__paeth(0, prior[k], 0)) & 255);
						break;
					case STBI__F_avg_first:
						cur[k] = raw[k];
						break;
					case STBI__F_paeth_first:
						cur[k] = raw[k];
						break;
				}

			if (depth == 8)
			{
				if (img_n != out_n)
					cur[img_n] = 255;
				raw += img_n;
				cur += out_n;
				prior += out_n;
			}
			else if (depth == 16)
			{
				if (img_n != out_n)
				{
					cur[filter_shorts] = 255;
					cur[filter_shorts + 1] = 255;
				}

				raw += filter_shorts;
				cur += output_shorts;
				prior += output_shorts;
			}
			else
			{
				raw += 1;
				cur += 1;
				prior += 1;
			}

			if (depth < 8 || img_n == out_n)
			{
				var nk = (width - 1) * filter_shorts;
				switch (filter)
				{
					case STBI__F_none:
						FakePtr<Short>.memcpy(cur, raw, nk);
						break;
					case STBI__F_sub:
						for (k = 0; k < nk; ++k) cur[k] = (short)((raw[k] + cur[k - filter_shorts]) & 255);
						break;
					case STBI__F_up:
						for (k = 0; k < nk; ++k) cur[k] = (short)((raw[k] + prior[k]) & 255);
						break;
					case STBI__F_avg:
						for (k = 0; k < nk; ++k)
							cur[k] = (short)((raw[k] + ((prior[k] + cur[k - filter_shorts]) >> 1)) & 255);
						break;
					case STBI__F_paeth:
						for (k = 0; k < nk; ++k)
							cur[k] = (short)((raw[k] + stbi__paeth(cur[k - filter_shorts], prior[k],
												  prior[k - filter_shorts])) & 255);
						break;
					case STBI__F_avg_first:
						for (k = 0; k < nk; ++k) cur[k] = (short)((raw[k] + (cur[k - filter_shorts] >> 1)) & 255);
						break;
					case STBI__F_paeth_first:
						for (k = 0; k < nk; ++k)
							cur[k] = (short)((raw[k] + stbi__paeth(cur[k - filter_shorts], 0, 0)) & 255);
						break;
				}

				raw += nk;
			}
			else
			{
				switch (filter)
				{
					case STBI__F_none:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = raw[k];
						break;
					case STBI__F_sub:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = (short)((raw[k] + cur[k - output_shorts]) & 255);
						break;
					case STBI__F_up:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = (short)((raw[k] + prior[k]) & 255);
						break;
					case STBI__F_avg:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = (short)((raw[k] + ((prior[k] + cur[k - output_shorts]) >> 1)) & 255);
						break;
					case STBI__F_paeth:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = (short)((raw[k] + stbi__paeth(cur[k - output_shorts], prior[k],
													  prior[k - output_shorts])) & 255);
						break;
					case STBI__F_avg_first:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = (short)((raw[k] + (cur[k - output_shorts] >> 1)) & 255);
						break;
					case STBI__F_paeth_first:
						for (i = x - 1;
							i >= 1;
							--i, cur[filter_shorts] = (short)255, raw += filter_shorts, cur += output_shorts, prior +=
								output_shorts)
							for (k = 0; k < filter_shorts; ++k)
								cur[k] = (short)((raw[k] + stbi__paeth(cur[k - output_shorts], 0, 0)) & 255);
						break;
				}

				if (depth == 16)
				{
					cur = ptr + stride * j;
					for (i = (long)0; i < x; ++i, cur += output_shorts) cur[filter_shorts + 1] = 255;
				}
			}
		}

		if (depth < 8)
			for (j = (long)0; j < y; ++j)
			{
				var cur = ptr + stride * j;
				var _in_ = ptr + stride * j + x * out_n - img_width_shorts;
				var scale = (short)(color == 0 ? stbi__depth_scale_table[depth] : 1);
				if (depth == 4)
				{
					for (k = (int)(x * img_n); k >= 2; k -= 2, ++_in_)
					{
						cur.SetAndIncrease((short)(scale * (_in_.Value >> 4)));
						cur.SetAndIncrease((short)(scale * (_in_.Value & 0x0f)));
					}

					if (k > 0)
						cur.SetAndIncrease((short)(scale * (_in_.Value >> 4)));
				}
				else if (depth == 2)
				{
					for (k = (int)(x * img_n); k >= 4; k -= 4, ++_in_)
					{
						cur.SetAndIncrease((short)(scale * (_in_.Value >> 6)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 4) & 0x03)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 2) & 0x03)));
						cur.SetAndIncrease((short)(scale * (_in_.Value & 0x03)));
					}

					if (k > 0)
						cur.SetAndIncrease((short)(scale * (_in_.Value >> 6)));
					if (k > 1)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 4) & 0x03)));
					if (k > 2)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 2) & 0x03)));
				}
				else if (depth == 1)
				{
					for (k = (int)(x * img_n); k >= 8; k -= 8, ++_in_)
					{
						cur.SetAndIncrease((short)(scale * (_in_.Value >> 7)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 6) & 0x01)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 5) & 0x01)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 4) & 0x01)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 3) & 0x01)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 2) & 0x01)));
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 1) & 0x01)));
						cur.SetAndIncrease((short)(scale * (_in_.Value & 0x01)));
					}

					if (k > 0)
						cur.SetAndIncrease((short)(scale * (_in_.Value >> 7)));
					if (k > 1)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 6) & 0x01)));
					if (k > 2)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 5) & 0x01)));
					if (k > 3)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 4) & 0x01)));
					if (k > 4)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 3) & 0x01)));
					if (k > 5)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 2) & 0x01)));
					if (k > 6)
						cur.SetAndIncrease((short)(scale * ((_in_.Value >> 1) & 0x01)));
				}

				if (img_n != out_n)
				{
					var q = 0;
					cur = ptr + stride * j;
					if (img_n == 1)
						for (q = (int)(x - 1); q >= 0; --q)
						{
							cur[q * 2 + 1] = 255;
							cur[q * 2 + 0] = cur[q];
						}
					else
						for (q = (int)(x - 1); q >= 0; --q)
						{
							cur[q * 4 + 3] = 255;
							cur[q * 4 + 2] = cur[q * 3 + 2];
							cur[q * 4 + 1] = cur[q * 3 + 1];
							cur[q * 4 + 0] = cur[q * 3 + 0];
						}
				}
			}
		else if (depth == 16)
			throw new UnsupportedOperationException();
		/*				FakePtr<Short> cur = ptr;
						int* cur16 = (int*)(cur);
						for (i = (long)(0); (i) < (x * y * out_n); ++i, cur16++, cur += 2)
						{
							*cur16 = (int)((cur[0] << 8) | cur[1]);
						}*/

/*		return 1;
	}

	private int stbi__create_png_image(FakePtr<Short> image_data, long image_data_len, int out_n, int depth,
		int color, int interlaced)
	{
		var shorts = depth == 16 ? 2 : 1;
		var out_shorts = out_n * shorts;
		var p = 0;
		if (interlaced == 0)
			return stbi__create_png_image_raw(image_data, image_data_len, out_n, (long)img_x, (long)img_y, depth,
				color);
		var final = new short[img_x * img_y * out_shorts];
		var xorig = new int[7];
		var yorig = new int[7];
		var xspc = new int[7];
		var yspc = new int[7];

		for (p = 0; p < 7; ++p)
		{
			xorig[0] = 0;
			xorig[1] = 4;
			xorig[2] = 0;
			xorig[3] = 2;
			xorig[4] = 0;
			xorig[5] = 1;
			xorig[6] = 0;

			yorig[0] = 0;
			yorig[1] = 0;
			yorig[2] = 4;
			yorig[3] = 0;
			yorig[4] = 2;
			yorig[5] = 0;
			yorig[6] = 1;

			xspc[0] = 8;
			xspc[1] = 8;
			xspc[2] = 4;
			xspc[3] = 4;
			xspc[4] = 2;
			xspc[5] = 2;
			xspc[6] = 1;

			yspc[0] = 8;
			yspc[1] = 8;
			yspc[2] = 8;
			yspc[3] = 4;
			yspc[4] = 4;
			yspc[5] = 2;
			yspc[6] = 2;
			var i = 0;
			var j = 0;
			var x = 0;
			var y = 0;
			x = (img_x - xorig[p] + xspc[p] - 1) / xspc[p];
			y = (img_y - yorig[p] + yspc[p] - 1) / yspc[p];
			if (x != 0 && y != 0)
			{
				var img_len = (long)((((img_n * x * depth + 7) >> 3) + 1) * y);
				if (stbi__create_png_image_raw(image_data, image_data_len, out_n, (long)x, (long)y, depth,
						color) == 0) return 0;

				var finalPtr = new FakePtr<Short>(final);
				var outPtr = new FakePtr<Short>(_out_);
				for (j = 0; j < y; ++j)
					for (i = 0; i < x; ++i)
					{
						var out_y = j * yspc[p] + yorig[p];
						var out_x = i * xspc[p] + xorig[p];
						FakePtr<Short>.memcpy(finalPtr + out_y * img_x * out_shorts + out_x * out_shorts,
							outPtr + (j * x + i) * out_shorts,
							out_shorts);
					}

				image_data += img_len;
				image_data_len -= img_len;
			}
		}

		_out_ = final;
		return 1;
	}

	private int stbi__compute_transparency(short[] tc, int out_n)
	{
		long i = 0;
		var pixel_count = (long)(img_x * img_y);
		var p = new FakePtr<Short>(_out_);
		if (out_n == 2)
			for (i = (long)0; i < pixel_count; ++i)
			{
				p[1] = (short)(p[0] == tc[0] ? 0 : 255);
				p += 2;
			}
		else
			for (i = (long)0; i < pixel_count; ++i)
			{
				if (p[0] == tc[0] && p[1] == tc[1] && p[2] == tc[2])
					p[3] = 0;
				p += 4;
			}

		return 1;
	}

	private int stbi__compute_transparency16(int[] tc, int out_n)
	{
		throw new UnsupportedOperationException();

		/*			long i = 0;
					long pixel_count = (long)(img_x * img_y);
					FakePtr<Integer> p = new FakePtr<Integer>(_out_);
					if ((out_n) == (2))
					{
						for (i = (long)(0); (i) < (pixel_count); ++i)
						{
							p[1] = (int)((p[0]) == (tc[0]) ? 0 : 65535);
							p += 2;
						}
					}
					else
					{
						for (i = (long)(0); (i) < (pixel_count); ++i)
						{
							if ((((p[0]) == (tc[0])) && ((p[1]) == (tc[1]))) && ((p[2]) == (tc[2])))
								p[3] = (int)(0);
							p += 4;
						}
					}

					return (int)(1);*/
/*	}

	private int stbi__expand_png_palette(short[] palette, int len, int pal_img_n)
	{
		long i = 0;
		var pixel_count = (long)(img_x * img_y);
		var orig = _out_;
		_out_ = new short[pixel_count * pal_img_n];
		var p = new FakePtr<Short>(_out_);
		if (pal_img_n == 3)
			for (i = (long)0; i < pixel_count; ++i)
			{
				var n = orig[i] * 4;
				p[0] = palette[n];
				p[1] = palette[n + 1];
				p[2] = palette[n + 2];
				p += 3;
			}
		else
			for (i = (long)0; i < pixel_count; ++i)
			{
				var n = orig[i] * 4;
				p[0] = palette[n];
				p[1] = palette[n + 1];
				p[2] = palette[n + 2];
				p[3] = palette[n + 3];
				p += 4;
			}

		return 1;
	}

	private void stbi_set_unpremultiply_on_load(int flag_true_if_should_unpremultiply)
	{
		stbi__unpremultiply_on_load = flag_true_if_should_unpremultiply;
	}

	private void stbi_convert_iphone_png_to_rgb(int flag_true_if_should_convert)
	{
		stbi__de_iphone_flag = flag_true_if_should_convert;
	}

	private void stbi__de_iphone()
	{
		long i = 0;
		var pixel_count = (long)(img_x * img_y);
		var p = new FakePtr<Short>(_out_);
		if (img_out_n == 3)
		{
			for (i = (long)0; i < pixel_count; ++i)
			{
				var t = p[0];
				p[0] = p[2];
				p[2] = t;
				p += 3;
			}
		}
		else
		{
			if (stbi__unpremultiply_on_load != 0)
				for (i = (long)0; i < pixel_count; ++i)
				{
					var a = p[3];
					var t = p[0];
					if (a != 0)
					{
						var half = (short)(a / 2);
						p[0] = (short)((p[2] * 255 + half) / a);
						p[1] = (short)((p[1] * 255 + half) / a);
						p[2] = (short)((t * 255 + half) / a);
					}
					else
					{
						p[0] = p[2];
						p[2] = t;
					}

					p += 4;
				}
			else
				for (i = (long)0; i < pixel_count; ++i)
				{
					var t = p[0];
					p[0] = p[2];
					p[2] = t;
					p += 4;
				}
		}
	}

	private int stbi__parse_png_file(int scan, int req_comp) throws Exception
	{
		var palette = new short[1024];
		var pal_img_n = (short)0;
		var has_trans = (short)0;
		var tc = new short[3];
		tc[0] = 0;

		var tc16 = new int[3];
		var ioff = 0;
		var idata_limit = 0;
		long i = 0;
		var pal_len = (long)0;
		var first = 1;
		var k = 0;
		var interlace = 0;
		var color = 0;
		var is_iphone = 0;
		expanded = null;
		idata = null;
		_out_ = null;
		if (!stbi__check_png_header(InputStream))
			return 0;
		if (scan == STBI__SCAN_type)
			return 1;
		for (; ; )
		{
			var c = stbi__get_chunk_header();
			switch (c.type)
			{
				case ((long)'C' << 24) + ((long)'g' << 16) + ((long)'B' << 8) + 'I':
					is_iphone = 1;
					stbi__skip((int)c.length);
					break;
				case ((long)'I' << 24) + ((long)'H' << 16) + ((long)'D' << 8) + 'R':
				{
					var comp = 0;
					var filter = 0;
					if (first == 0)
						stbi__err("multiple IHDR");
					first = 0;
					if (c.length != 13)
						stbi__err("bad IHDR len");
					img_x = (int)stbi__get32be();
					if (img_x > 1 << 24)
						stbi__err("too large");
					img_y = (int)stbi__get32be();
					if (img_y > 1 << 24)
						stbi__err("too large");
					depth = stbi__get8();
					if (depth != 1 && depth != 2 && depth != 4 && depth != 8 && depth != 16)
						stbi__err("1/2/4/8/16-bit only");
					color = stbi__get8();
					if (color > 6)
						stbi__err("bad ctype");
					if (color == 3 && depth == 16)
						stbi__err("bad ctype");
					if (color == 3)
						pal_img_n = 3;
					else if ((color & 1) != 0)
						stbi__err("bad ctype");
					comp = stbi__get8();
					if (comp != 0)
						stbi__err("bad comp method");
					filter = stbi__get8();
					if (filter != 0)
						stbi__err("bad filter method");
					interlace = stbi__get8();
					if (interlace > 1)
						stbi__err("bad interlace method");
					if (img_x == 0 || img_y == 0)
						stbi__err("0-pixel image");
					if (pal_img_n == 0)
					{
						img_n = ((color & 2) != 0 ? 3 : 1) + ((color & 4) != 0 ? 1 : 0);
						if ((1 << 30) / img_x / img_n < img_y)
							stbi__err("too large");
						if (scan == STBI__SCAN_header)
							return 1;
					}
					else
					{
						img_n = 1;
						if ((1 << 30) / img_x / 4 < img_y)
							stbi__err("too large");
					}

					break;
				}
				case ((long)'P' << 24) + ((long)'L' << 16) + ((long)'T' << 8) + 'E':
				{
					if (first != 0)
						stbi__err("first not IHDR");
					if (c.length > 256 * 3)
						stbi__err("invalid PLTE");
					pal_len = c.length / 3;
					if (pal_len * 3 != c.length)
						stbi__err("invalid PLTE");
					for (i = (long)0; i < pal_len; ++i)
					{
						palette[i * 4 + 0] = stbi__get8();
						palette[i * 4 + 1] = stbi__get8();
						palette[i * 4 + 2] = stbi__get8();
						palette[i * 4 + 3] = 255;
					}

					break;
				}
				case ((long)'t' << 24) + ((long)'R' << 16) + ((long)'N' << 8) + 'S':
				{
					if (first != 0)
						stbi__err("first not IHDR");
					if (idata != null)
						stbi__err("tRNS after IDAT");
					if (pal_img_n != 0)
					{
						if (scan == STBI__SCAN_header)
						{
							img_n = 4;
							return 1;
						}

						if (pal_len == 0)
							stbi__err("tRNS before PLTE");
						if (c.length > pal_len)
							stbi__err("bad tRNS len");
						pal_img_n = 4;
						for (i = (long)0; i < c.length; ++i) palette[i * 4 + 3] = stbi__get8();
					}
					else
					{
						if ((img_n & 1) == 0)
							stbi__err("tRNS with alpha");
						if (c.length != (long)img_n * 2)
							stbi__err("bad tRNS len");
						has_trans = 1;
						if (depth == 16)
							for (k = 0; k < img_n; ++k)
								tc16[k] = (int)stbi__get16be();
						else
							for (k = 0; k < img_n; ++k)
								tc[k] = (short)((short)(stbi__get16be() & 255) * stbi__depth_scale_table[depth]);
					}

					break;
				}
				case ((long)'I' << 24) + ((long)'D' << 16) + ((long)'A' << 8) + 'T':
				{
					if (first != 0)
						stbi__err("first not IHDR");
					if (pal_img_n != 0 && pal_len == 0)
						stbi__err("no PLTE");
					if (scan == STBI__SCAN_header)
					{
						img_n = pal_img_n;
						return 1;
					}

					if ((int)(ioff + c.length) < ioff)
						return 0;
					if (ioff + c.length > idata_limit)
					{
						var idata_limit_old = (long)idata_limit;
						if (idata_limit == 0)
							idata_limit = (int)(c.length > 4096 ? c.length : 4096);
						while (ioff + c.length > idata_limit) idata_limit *= 2;

						Array.Resize(ref idata, idata_limit);
					}

					if (!stbi__getn(idata, ioff, (int)c.length))
						stbi__err("outofdata");
					ioff += (int)c.length;
					break;
				}
				case ((long)'I' << 24) + ((long)'E' << 16) + ((long)'N' << 8) + 'D':
				{
					var raw_len = 0;
					long bpl = 0;
					if (first != 0)
						stbi__err("first not IHDR");
					if (scan != STBI__SCAN_load)
						return 1;
					if (idata == null)
						stbi__err("no IDAT");
					bpl = (long)((img_x * depth + 7) / 8);
					raw_len = (int)(bpl * img_y * img_n + img_y);
					expanded = ZLib.stbi_zlib_decode_malloc_guesssize_headerflag(idata, ioff, raw_len, out raw_len,
						is_iphone != 0 ? 0 : 1);
					if (expanded == null)
						return 0;
					idata = null;
					if (req_comp == img_n + 1 && req_comp != 3 && pal_img_n == 0 || has_trans != 0)
						img_out_n = img_n + 1;
					else
						img_out_n = img_n;
					if (stbi__create_png_image(new FakePtr<Short>(expanded), (long)raw_len, img_out_n, depth, color,
							interlace) == 0)
						return 0;
					if (has_trans != 0)
					{
						if (depth == 16)
						{
							if (stbi__compute_transparency16(tc16, img_out_n) == 0)
								return 0;
						}
						else
						{
							if (stbi__compute_transparency(tc, img_out_n) == 0)
								return 0;
						}
					}

					if (is_iphone != 0 && stbi__de_iphone_flag != 0 && img_out_n > 2)
						stbi__de_iphone();
					if (pal_img_n != 0)
					{
						img_n = pal_img_n;
						img_out_n = pal_img_n;
						if (req_comp >= 3)
							img_out_n = req_comp;
						if (stbi__expand_png_palette(palette, (int)pal_len, img_out_n) == 0)
							return 0;
					}
					else if (has_trans != 0)
					{
						++img_n;
					}

					expanded = null;
					return 1;
				}
				default:
					if (first != 0)
						stbi__err("first not IHDR");
					if ((c.type & (1 << 29)) == 0)
					{
						var invalid_chunk = c.type + " PNG chunk not known";
						stbi__err(invalid_chunk);
					}

					stbi__skip((int)c.length);
					break;
			}

			stbi__get32be();
		}
	}

	private ImageResult InternalDecode(ColorComponents requiredComponents)
	{
		var req_comp = requiredComponents.ToReqComp();
		if (req_comp < 0 || req_comp > 4)
			stbi__err("bad req_comp");

		try
		{
			if (stbi__parse_png_file(STBI__SCAN_load, req_comp) == 0) stbi__err("could not parse png");

			var bits_per_channel = 8;
			if (depth < 8)
				bits_per_channel = 8;
			else
				bits_per_channel = depth;
			var result = _out_;
			_out_ = null;
			if (req_comp != 0 && req_comp != img_out_n)
			{
				if (bits_per_channel == 8)
					result = Utility.stbi__convert_format(result, img_out_n, req_comp, (long)img_x,
						(long)img_y);
				else
					result = Utility.stbi__convert_format16(result, img_out_n, req_comp, (long)img_x,
						(long)img_y);
				img_out_n = req_comp;
			}

			return new ImageResult
			{
				Width = img_x,
				Height = img_y,
				SourceComponents = (ColorComponents)img_n,
				ColorComponents = requiredComponents != null ? requiredComponents.Value : (ColorComponents)img_n,
				BitsPerChannel = bits_per_channel,
				Data = result
			};
		}
		finally
		{
			_out_ = null;
			expanded = null;
			idata = null;
		}
	}

	public static boolean Test(InputStream stream)
	{
		var r = stbi__check_png_header(stream);
		stream.Rewind();

		return r;
	}

	public static ImageInfo Info(InputStream stream)
	{
		var decoder = new PngDecoder(stream);
		var r = decoder.stbi__parse_png_file(STBI__SCAN_header, 0);
		stream.Rewind();

		if (r == 0) return null;

		return new ImageInfo
		{
			Width = decoder.img_x,
			Height = decoder.img_y,
			ColorComponents = (ColorComponents)decoder.img_n,
			BitsPerChannel = decoder.depth
		};
	}

	public static ImageResult Decode(InputStream stream, ColorComponents  requiredComponents = null)
	{
		var decoder = new PngDecoder(stream);
		return decoder.InternalDecode(requiredComponents);
	}
}*/