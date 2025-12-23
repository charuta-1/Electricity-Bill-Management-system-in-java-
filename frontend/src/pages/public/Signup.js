import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Card, Col, Form, Row } from 'react-bootstrap';
import { useAuth } from '../../context/AuthContext.js';
import logo from '../../assets/logo-vit-energysuite.png';

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{8,}$/;
const phonePattern = /^\d{10}$/;
const pincodePattern = /^\d{6}$/;
const aadharPattern = /^\d{12}$/;

const initialForm = {
	username: '',
	password: '',
	confirmPassword: '',
	email: '',
	fullName: '',
	phoneNumber: '',
	address: '',
	city: '',
	state: 'Maharashtra',
	pincode: '',
	aadharNumber: ''
};

const Signup = () => {
	const [formData, setFormData] = useState({ ...initialForm });
	const [error, setError] = useState('');
	const [success, setSuccess] = useState('');
	const [submitting, setSubmitting] = useState(false);
	const navigate = useNavigate();
	const { register, user } = useAuth();

	useEffect(() => {
		if (user) {
			const landing = user.role === 'ADMIN' ? '/admin/dashboard' : '/customer/dashboard';
			navigate(landing, { replace: true });
		}
	}, [user, navigate]);

	const normalizedPayload = useMemo(
		() => ({
			username: formData.username.trim().toLowerCase(),
			password: formData.password,
			email: formData.email.trim().toLowerCase(),
			fullName: formData.fullName.trim(),
			phoneNumber: formData.phoneNumber.trim(),
			address: formData.address.trim(),
			city: formData.city.trim(),
			state: formData.state.trim() || 'Maharashtra',
			pincode: formData.pincode.trim(),
			aadharNumber: formData.aadharNumber.trim() || null
		}),
		[formData]
	);

	const handleChange = (event) => {
		const { name, value } = event.target;
		setFormData((prev) => ({
			...prev,
			[name]: value
		}));
	};

	const validateForm = () => {
		if (!normalizedPayload.fullName) {
			return 'Full name is required';
		}
		if (!normalizedPayload.username || normalizedPayload.username.length < 3) {
			return 'Username must be at least 3 characters';
		}
		if (!passwordPattern.test(formData.password)) {
			return 'Password must include upper, lower, digit, special character and be at least 8 characters long';
		}
		if (formData.password !== formData.confirmPassword) {
			return 'Passwords do not match';
		}
		if (!normalizedPayload.email) {
			return 'Email is required';
		}
		if (!phonePattern.test(normalizedPayload.phoneNumber)) {
			return 'Phone number must be 10 digits';
		}
		if (!normalizedPayload.address) {
			return 'Address is required';
		}
		if (!normalizedPayload.city) {
			return 'City is required';
		}
		if (!pincodePattern.test(normalizedPayload.pincode)) {
			return 'Pincode must be 6 digits';
		}
		if (formData.aadharNumber.trim() && !aadharPattern.test(formData.aadharNumber.trim())) {
			return 'Aadhar number must be 12 digits';
		}
		return null;
	};

	const handleSubmit = async (event) => {
		event.preventDefault();
		setError('');
		setSuccess('');
		setSubmitting(true);

		const validationMessage = validateForm();
		if (validationMessage) {
			setError(validationMessage);
			setSubmitting(false);
			return;
		}

		try {
			const result = await register(normalizedPayload);
			if (result.success) {
				setSuccess('Account created successfully! Redirecting to your dashboard...');
				setFormData({ ...initialForm });
			} else {
				setError(result.message || 'Unable to create account');
			}
		} finally {
			setSubmitting(false);
		}
	};

	return (
		<div className="auth-wrapper">
			<Row className="w-100 align-items-stretch" style={{ maxWidth: '1100px' }}>
				<Col md={6} className="mb-4 mb-md-0">
					<div className="auth-hero">
						<img src={logo} alt="VIT EnergySuite" style={{ width: '160px', margin: '0 auto' }} />
						<h1>Create Your Account</h1>
						<small>Activate digital billing for your VIT EnergySuite services.</small>
						<p className="mb-0 small">
							Already registered? <Link to="/login">Sign in instead</Link>.
						</p>
					</div>
				</Col>
				<Col md={6}>
					<Card className="auth-card border-0">
						<div className="text-center mb-4">
							<img src={logo} alt="VIT EnergySuite Logo" style={{ width: '180px' }} />
						</div>
						<h3 className="text-center mb-2">Create Account</h3>
						<p className="text-center text-muted mb-4">Fill in your details to enable EnergySuite access.</p>

						{error && <div className="alert alert-danger">{error}</div>}
						{success && <div className="alert alert-success">{success}</div>}

						<Form onSubmit={handleSubmit} className="d-flex flex-column gap-3">
							<Row className="g-3">
								<Col md={6}>
									<Form.Group controlId="fullName">
										<Form.Label>Full Name</Form.Label>
										<Form.Control type="text" name="fullName" value={formData.fullName} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={6}>
									<Form.Group controlId="username">
										<Form.Label>Username</Form.Label>
										<Form.Control type="text" name="username" value={formData.username} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={6}>
									<Form.Group controlId="email">
										<Form.Label>Email</Form.Label>
										<Form.Control type="email" name="email" value={formData.email} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={6}>
									<Form.Group controlId="phoneNumber">
										<Form.Label>Phone Number</Form.Label>
										<Form.Control type="tel" name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={6}>
									<Form.Group controlId="password">
										<Form.Label>Password</Form.Label>
										<Form.Control type="password" name="password" value={formData.password} onChange={handleChange} required />
										<Form.Text className="text-muted">Must include upper, lower, digit & symbol (min 8 chars).</Form.Text>
									</Form.Group>
								</Col>
								<Col md={6}>
									<Form.Group controlId="confirmPassword">
										<Form.Label>Confirm Password</Form.Label>
										<Form.Control
											type="password"
											name="confirmPassword"
											value={formData.confirmPassword}
											onChange={handleChange}
											required
										/>
									</Form.Group>
								</Col>
								<Col xs={12}>
									<Form.Group controlId="address">
										<Form.Label>Service Address</Form.Label>
										<Form.Control as="textarea" rows={3} name="address" value={formData.address} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={4}>
									<Form.Group controlId="city">
										<Form.Label>City</Form.Label>
										<Form.Control type="text" name="city" value={formData.city} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={4}>
									<Form.Group controlId="state">
										<Form.Label>State</Form.Label>
										<Form.Control type="text" name="state" value={formData.state} onChange={handleChange} />
									</Form.Group>
								</Col>
								<Col md={4}>
									<Form.Group controlId="pincode">
										<Form.Label>Pincode</Form.Label>
										<Form.Control type="text" name="pincode" value={formData.pincode} onChange={handleChange} required />
									</Form.Group>
								</Col>
								<Col md={6}>
									<Form.Group controlId="aadharNumber">
										<Form.Label>Aadhar Number (optional)</Form.Label>
										<Form.Control type="text" name="aadharNumber" value={formData.aadharNumber} onChange={handleChange} />
									</Form.Group>
								</Col>
							</Row>

							<Button type="submit" variant="primary" className="w-100" size="lg" disabled={submitting}>
								{submitting ? 'Creating account…' : 'Create account'}
							</Button>
						</Form>

						<div className="text-center text-muted mt-4">
							Already have an account? <Link to="/login">Sign in</Link>
						</div>
					</Card>
				</Col>
			</Row>
		</div>
	);
};

export default Signup;
