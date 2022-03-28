package kit

import "sync"

type stack struct {
	mut  sync.Mutex
	nums []any
}

func NewStack() *stack {
	return &stack{nums: make([]any, 0)}
}

func (s *stack) Push(t any) {
	s.mut.Lock()
	defer s.mut.Unlock()

	s.nums = append(s.nums, t)
}

func (s *stack) Pop() any {
	s.mut.Lock()
	defer s.mut.Unlock()

	res := s.nums[len(s.nums)-1]
	s.nums = s.nums[:len(s.nums)-1]
	return res
}

func (s *stack) Len() int {
	s.mut.Lock()
	defer s.mut.Unlock()

	return len(s.nums)
}

func (s *stack) IsEmpty() bool {
	return s.Len() == 0
}
