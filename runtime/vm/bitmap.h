// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_BITMAP_H_
#define VM_BITMAP_H_

#include "vm/allocation.h"

namespace dart {

// Forward declarations.
class RawStackmap;
class Stackmap;


// BitmapBuilder is used to build a bitmap. The implementation is optimized for
// a dense set of small bit maps without an upper bound (e.g: a pointer map
// description of a stack).
class BitmapBuilder : public ZoneAllocated {
 public:
  BitmapBuilder() : size_in_bytes_(kInitialSizeInBytes),
                    bit_list_(bit_list_data_) {
    memset(bit_list_data_, 0, kInitialSizeInBytes);
  }

  intptr_t SizeInBits() const { return (size_in_bytes_ * kBitsPerByte); }
  intptr_t SizeInBytes() const { return size_in_bytes_; }

  // Get/Set individual bits in the bitmap, set expands the underlying bitmap
  // if needed.
  bool Get(intptr_t bit_offset) const;
  void Set(intptr_t bit_offset, bool value);

  // Return the bit offset of the highest bit set.
  intptr_t Maximum() const;

  // Return the bit offset of the lowest bit set.
  intptr_t Minimum() const;

  // Sets min..max (inclusive) to value.
  void SetRange(intptr_t min, intptr_t max, bool value);

  // Replicates the bit map setting of the passed in Stackmap object.
  void SetBits(const Stackmap& bitmap);

 private:
  static const intptr_t kInitialSizeInBytes = 16;
  static const intptr_t kIncrementSizeInBytes = 16;

  bool InRange(intptr_t offset) const {
    return (offset >= 0) && (offset < SizeInBits());
  }

  bool GetBit(intptr_t bit_offset) const;
  void SetBit(intptr_t bit_offset, bool value);

  intptr_t size_in_bytes_;
  uint8_t bit_list_data_[kInitialSizeInBytes];
  uint8_t* bit_list_;

  DISALLOW_COPY_AND_ASSIGN(BitmapBuilder);
};

}  // namespace dart

#endif  // VM_BITMAP_H_
