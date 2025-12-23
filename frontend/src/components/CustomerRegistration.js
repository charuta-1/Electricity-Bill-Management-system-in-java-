import React, { useState, useEffect } from 'react';
import { Form, Button, Container, Row, Col } from 'react-bootstrap';
import axios from '../api/axiosConfig';

const CustomerRegistration = () => {
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
        address: '',
        areaId: '',
        advancePayment: 0
    });

    const [areas, setAreas] = useState([]);

    useEffect(() => {
        // Fetch areas when component mounts
        const fetchAreas = async () => {
            try {
                const response = await axios.get('/api/areas');
                setAreas(response.data);
            } catch (error) {
                console.error('Error fetching areas:', error);
            }
        };
        fetchAreas();
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await axios.post('/api/customers', formData);
            alert('Customer registered successfully!');
            // Reset form or redirect
        } catch (error) {
            console.error('Error registering customer:', error);
            alert('Error registering customer');
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    return (
        <Container className="mt-4">
            <h2>Customer Registration</h2>
            <Form onSubmit={handleSubmit}>
                <Row>
                    <Col md={6}>
                        <Form.Group className="mb-3">
                            <Form.Label>Name</Form.Label>
                            <Form.Control
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                required
                            />
                        </Form.Group>
                    </Col>
                    <Col md={6}>
                        <Form.Group className="mb-3">
                            <Form.Label>Email</Form.Label>
                            <Form.Control
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                            />
                        </Form.Group>
                    </Col>
                </Row>

                <Row>
                    <Col md={6}>
                        <Form.Group className="mb-3">
                            <Form.Label>Phone</Form.Label>
                            <Form.Control
                                type="tel"
                                name="phone"
                                value={formData.phone}
                                onChange={handleChange}
                                required
                            />
                        </Form.Group>
                    </Col>
                    <Col md={6}>
                        <Form.Group className="mb-3">
                            <Form.Label>Area</Form.Label>
                            <Form.Select
                                name="areaId"
                                value={formData.areaId}
                                onChange={handleChange}
                                required
                            >
                                <option value="">Select Area</option>
                                {areas.map(area => (
                                    <option key={area.id} value={area.id}>
                                        {area.areaName}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </Col>
                </Row>

                <Row>
                    <Col md={12}>
                        <Form.Group className="mb-3">
                            <Form.Label>Address</Form.Label>
                            <Form.Control
                                as="textarea"
                                name="address"
                                value={formData.address}
                                onChange={handleChange}
                                required
                            />
                        </Form.Group>
                    </Col>
                </Row>

                <Row>
                    <Col md={6}>
                        <Form.Group className="mb-3">
                            <Form.Label>Advance Payment (Optional)</Form.Label>
                            <Form.Control
                                type="number"
                                name="advancePayment"
                                value={formData.advancePayment}
                                onChange={handleChange}
                                min="0"
                                step="0.01"
                            />
                        </Form.Group>
                    </Col>
                </Row>

                <Button variant="primary" type="submit">
                    Register Customer
                </Button>
            </Form>
        </Container>
    );
};

export default CustomerRegistration;